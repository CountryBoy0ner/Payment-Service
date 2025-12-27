package com.innowise.service.impl;

import com.innowise.dto.CreatePaymentRequest;
import com.innowise.dto.PaymentDto;
import com.innowise.integration.RandomNumberClient;
import com.innowise.kafka.event.CreatePaymentEvent;
import com.innowise.kafka.producer.PaymentEventProducer;
import com.innowise.mapper.PaymentMapper;
import com.innowise.model.Payment;
import com.innowise.model.PaymentStatus;
import com.innowise.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository repo;
    @Mock
    private PaymentMapper mapper;
    @Mock
    private RandomNumberClient randomClient;
    @Mock
    private PaymentEventProducer producer;

    @InjectMocks
    private PaymentServiceImpl service;

    private CreatePaymentRequest req;
    private Payment mappedEntity;

    @BeforeEach
    void setUp() {
        req = new CreatePaymentRequest();
        req.setOrderId(10L);
        req.setUserId(7L);
        req.setPaymentAmount(new BigDecimal("12.34"));

        mappedEntity = new Payment();
        mappedEntity.setOrderId(req.getOrderId());
        mappedEntity.setUserId(req.getUserId());
        mappedEntity.setPaymentAmount(req.getPaymentAmount());
    }

    // ======================= create() =======================

    @Test
    void create_whenExistingPaymentFound_shouldPublishEventAndReturnMappedDto() {
        OffsetDateTime ts = OffsetDateTime.parse("2025-12-26T12:00:00Z");
        Payment existing = payment("EXISTING_ID", 10L, 7L, new BigDecimal("12.34"), PaymentStatus.SUCCESS, ts);

        PaymentDto existingDto = new PaymentDto();
        existingDto.setId("EXISTING_ID");
        existingDto.setOrderId(10L);
        existingDto.setUserId(7L);
        existingDto.setPaymentAmount(new BigDecimal("12.34"));

        when(repo.findByOrderId(10L)).thenReturn(List.of(existing));
        when(mapper.toDto(existing)).thenReturn(existingDto);

        PaymentDto result = service.create(req);

        assertSame(existingDto, result);

        // verify calls
        verify(repo).findByOrderId(10L);
        verify(mapper).toDto(existing);
        ArgumentCaptor<CreatePaymentEvent> evCap = ArgumentCaptor.forClass(CreatePaymentEvent.class);
        verify(producer).sendCreatePayment(evCap.capture());
        verifyNoInteractions(randomClient);
        verify(mapper, never()).toEntity(any());
        verify(repo, never()).save(any());
        CreatePaymentEvent ev = evCap.getValue();

        // check event content (через рефлексию — чтобы не зависеть от record/POJO)

        assertEquals(existing.getId(), prop(ev, "getId", "id", "getPaymentId", "paymentId"));
        assertEquals(existing.getOrderId(), prop(ev, "getOrderId", "orderId"));
        assertEquals(existing.getUserId(), prop(ev, "getUserId", "userId"));
        assertEquals(existing.getStatus().name(), prop(ev, "getStatus", "status"));
        assertEquals(existing.getPaymentAmount(), prop(ev, "getPaymentAmount", "paymentAmount"));
        assertEquals(existing.getTimestamp(), prop(ev, "getTimestamp", "timestamp"));

        verifyNoMoreInteractions(repo, mapper, producer);
    }

    @Test
    void create_whenNoExisting_andRandomEven_shouldSetSuccess_setTimestamp_publishEvent_andReturnDto() {
        when(repo.findByOrderId(10L)).thenReturn(List.of());
        when(mapper.toEntity(req)).thenReturn(mappedEntity);
        when(randomClient.getRandomNumber()).thenReturn(2); // even => SUCCESS

        // capture payment saved
        ArgumentCaptor<Payment> payCap = ArgumentCaptor.forClass(Payment.class);
        when(repo.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            assertNotNull(p.getTimestamp(), "timestamp must be set when request timestamp is null");
            assertEquals(PaymentStatus.SUCCESS, p.getStatus(), "status must be SUCCESS for even random");
            p.setId("1");
            return p;
        });

        PaymentDto dto = new PaymentDto();
        dto.setId("1");
        dto.setOrderId(10L);
        dto.setUserId(7L);
        dto.setPaymentAmount(new BigDecimal("12.34"));

        when(mapper.toDto(any(Payment.class))).thenReturn(dto);

        PaymentDto result = service.create(req);

        assertSame(dto, result);

        verify(repo).findByOrderId(10L);
        verify(mapper).toEntity(req);
        verify(randomClient).getRandomNumber();
        verify(repo).save(payCap.capture());
        verify(mapper).toDto(any(Payment.class));

        Payment savedArg = payCap.getValue();
        assertEquals("1", savedArg.getId());
        assertNotNull(savedArg.getTimestamp());
        assertEquals(PaymentStatus.SUCCESS, savedArg.getStatus());

        // validate event matches saved payment

        ArgumentCaptor<CreatePaymentEvent> evCap = ArgumentCaptor.forClass(CreatePaymentEvent.class);
        verify(producer).sendCreatePayment(evCap.capture());
        CreatePaymentEvent ev = evCap.getValue();

        assertEquals(savedArg.getId(), prop(ev, "getId", "id", "getPaymentId", "paymentId"));
        assertEquals(savedArg.getOrderId(), prop(ev, "getOrderId", "orderId"));
        assertEquals(savedArg.getUserId(), prop(ev, "getUserId", "userId"));
        assertEquals(savedArg.getStatus().name(), prop(ev, "getStatus", "status"));
        assertEquals(savedArg.getPaymentAmount(), prop(ev, "getPaymentAmount", "paymentAmount"));
        assertEquals(savedArg.getTimestamp(), prop(ev, "getTimestamp", "timestamp"));

        verifyNoMoreInteractions(repo, mapper, randomClient, producer);
    }

    @Test
    void create_whenNoExisting_andRandomOdd_shouldSetFailed_useProvidedTimestamp_publishEvent_andReturnDto() {
        OffsetDateTime providedTs = OffsetDateTime.parse("2025-12-26T12:00:00Z");
        req.setTimestamp(providedTs);

        when(repo.findByOrderId(10L)).thenReturn(List.of());
        when(mapper.toEntity(req)).thenReturn(mappedEntity);
        when(randomClient.getRandomNumber()).thenReturn(3); // odd => FAILED

        ArgumentCaptor<Payment> payCap = ArgumentCaptor.forClass(Payment.class);
        when(repo.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            assertEquals(providedTs, p.getTimestamp(), "service must not override provided timestamp");
            assertEquals(PaymentStatus.FAILED, p.getStatus(), "status must be FAILED for odd random");
            p.setId("1");
            return p;
        });

        PaymentDto dto = new PaymentDto();
        dto.setId("1");
        dto.setOrderId(10L);
        dto.setUserId(7L);
        dto.setPaymentAmount(new BigDecimal("12.34"));
        when(mapper.toDto(any(Payment.class))).thenReturn(dto);

        PaymentDto result = service.create(req);

        assertSame(dto, result);

        verify(repo).findByOrderId(10L);
        verify(mapper).toEntity(req);
        verify(randomClient).getRandomNumber();
        verify(repo).save(payCap.capture());
        verify(producer).sendCreatePayment(any(CreatePaymentEvent.class));
        verify(mapper).toDto(any(Payment.class));

        Payment savedArg = payCap.getValue();
        assertEquals(PaymentStatus.FAILED, savedArg.getStatus());
        assertEquals(providedTs, savedArg.getTimestamp());

        ArgumentCaptor<CreatePaymentEvent> evCap = ArgumentCaptor.forClass(CreatePaymentEvent.class);
        verify(producer).sendCreatePayment(evCap.capture());
        CreatePaymentEvent ev = evCap.getValue();
        assertEquals("FAILED", prop(ev, "getStatus", "status"));

        verifyNoMoreInteractions(repo, mapper, randomClient, producer);
    }

    @Test
    void create_whenSaveThrowsDuplicateKey_andPaymentAlreadyCreatedInParallel_shouldFetchExisting_publishEvent_andReturnDto() {
        OffsetDateTime ts = OffsetDateTime.parse("2025-12-26T12:00:00Z");
        Payment already = payment("ALREADY_ID", 10L, 7L, new BigDecimal("12.34"), PaymentStatus.SUCCESS, ts);

        PaymentDto alreadyDto = new PaymentDto();
        alreadyDto.setId("ALREADY_ID");
        alreadyDto.setOrderId(10L);
        alreadyDto.setUserId(7L);
        alreadyDto.setPaymentAmount(new BigDecimal("12.34"));

        when(repo.findByOrderId(10L)).thenReturn(List.of(), List.of(already));
        when(mapper.toEntity(req)).thenReturn(mappedEntity);
        when(randomClient.getRandomNumber()).thenReturn(2);
        when(repo.save(any(Payment.class))).thenThrow(new DuplicateKeyException("dup"));
        when(mapper.toDto(already)).thenReturn(alreadyDto);

        PaymentDto result = service.create(req);

        assertSame(alreadyDto, result);

        verify(repo, times(2)).findByOrderId(10L);
        verify(mapper).toEntity(req);
        verify(randomClient).getRandomNumber();
        verify(repo).save(any(Payment.class));

        verify(producer).sendCreatePayment(any(CreatePaymentEvent.class));
        verify(mapper).toDto(already);

        // should NOT map/save-publish the failed attempt result (только already)
        verify(mapper, never()).toDto(argThat(p -> p != null && "1".equals(p.getId())));

        verifyNoMoreInteractions(repo, mapper, randomClient, producer);
    }

    @Test
    void create_whenSaveThrowsDuplicateKey_andExistingNotFoundAfterRetry_shouldRethrow_andNotPublish() {
        when(repo.findByOrderId(10L)).thenReturn(List.of(), List.of());
        when(mapper.toEntity(req)).thenReturn(mappedEntity);
        when(randomClient.getRandomNumber()).thenReturn(2);
        when(repo.save(any(Payment.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThrows(DuplicateKeyException.class, () -> service.create(req));

        verify(repo, times(2)).findByOrderId(10L);
        verify(mapper).toEntity(req);
        verify(randomClient).getRandomNumber();
        verify(repo).save(any(Payment.class));

        verifyNoInteractions(producer);
        verify(mapper, never()).toDto(any(Payment.class));

        verifyNoMoreInteractions(repo, mapper, randomClient);
    }

    // ======================= getByOrderId() =======================

    @Test
    void getByOrderId_shouldReturnMappedDtos() {
        Payment p1 = new Payment(); p1.setId("1");
        Payment p2 = new Payment(); p2.setId("2");

        PaymentDto d1 = new PaymentDto(); d1.setId("1");
        PaymentDto d2 = new PaymentDto(); d2.setId("2");

        when(repo.findByOrderId(10L)).thenReturn(List.of(p1, p2));
        when(mapper.toDto(p1)).thenReturn(d1);
        when(mapper.toDto(p2)).thenReturn(d2);

        List<PaymentDto> res = service.getByOrderId(10L);

        assertEquals(List.of(d1, d2), res);

        verify(repo).findByOrderId(10L);
        verify(mapper).toDto(p1);
        verify(mapper).toDto(p2);
        verifyNoMoreInteractions(repo, mapper);

        verifyNoInteractions(randomClient, producer);
    }

    // ======================= getByUserId() =======================

    @Test
    void getByUserId_shouldReturnMappedDtos() {
        Payment p1 = new Payment(); p1.setId("1");
        PaymentDto d1 = new PaymentDto(); d1.setId("1");

        when(repo.findByUserId(7L)).thenReturn(List.of(p1));
        when(mapper.toDto(p1)).thenReturn(d1);

        List<PaymentDto> res = service.getByUserId(7L);

        assertEquals(List.of(d1), res);

        verify(repo).findByUserId(7L);
        verify(mapper).toDto(p1);
        verifyNoMoreInteractions(repo, mapper);

        verifyNoInteractions(randomClient, producer);
    }

    // ======================= getByStatuses() =======================

    @Test
    void getByStatuses_shouldCallRepoAndReturnMappedDtos() {
        List<PaymentStatus> statuses = List.of(PaymentStatus.SUCCESS, PaymentStatus.FAILED);

        Payment p1 = new Payment(); p1.setId("1");
        PaymentDto d1 = new PaymentDto(); d1.setId("1");

        when(repo.findByStatusIn(statuses)).thenReturn(List.of(p1));
        when(mapper.toDto(p1)).thenReturn(d1);

        List<PaymentDto> res = service.getByStatuses(statuses);

        assertEquals(List.of(d1), res);

        verify(repo).findByStatusIn(statuses);
        verify(mapper).toDto(p1);
        verifyNoMoreInteractions(repo, mapper);

        verifyNoInteractions(randomClient, producer);
    }

    // ======================= totalSum() =======================

    @Test
    void totalSum_shouldDelegateToRepo() {
        OffsetDateTime from = OffsetDateTime.parse("2025-12-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2025-12-31T23:59:59Z");
        BigDecimal expected = new BigDecimal("123.45");

        when(repo.sumForPeriod(from, to)).thenReturn(expected);

        BigDecimal res = service.totalSum(from, to);

        assertEquals(expected, res);

        verify(repo).sumForPeriod(from, to);
        verifyNoMoreInteractions(repo);

        verifyNoInteractions(mapper, randomClient, producer);
    }

    // ======================= helpers =======================

    private static Payment payment(String id, Long orderId, Long userId, BigDecimal amount, PaymentStatus status, OffsetDateTime ts) {
        Payment p = new Payment();
        p.setId(id);
        p.setOrderId(orderId);
        p.setUserId(userId);
        p.setPaymentAmount(amount);
        p.setStatus(status);
        p.setTimestamp(ts);
        return p;
    }

    /**
     * Достаёт значение из CreatePaymentEvent без знания его типа (record/POJO).
     * Пробует вызвать методы по списку имён: "getX", "x" и т.п.
     */
    @SuppressWarnings("unchecked")
    private static <T> T prop(Object target, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                return (T) m.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // try next
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError("No matching accessor in " + target.getClass().getName());
    }
}
