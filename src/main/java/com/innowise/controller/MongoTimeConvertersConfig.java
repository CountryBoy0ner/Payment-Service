package com.innowise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

@Configuration
public class MongoTimeConvertersConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new OffsetDateTimeToDate(),
                new DateToOffsetDateTime()
        ));
    }

    @WritingConverter
    static class OffsetDateTimeToDate implements Converter<OffsetDateTime, Date> {
        @Override
        public Date convert(OffsetDateTime source) {
            return source == null ? null : Date.from(source.toInstant());
        }
    }

    @ReadingConverter
    static class DateToOffsetDateTime implements Converter<Date, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(Date source) {
            return source == null ? null : source.toInstant().atOffset(ZoneOffset.UTC);
        }
    }
}
