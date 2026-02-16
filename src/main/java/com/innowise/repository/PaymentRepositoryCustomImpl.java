package com.innowise.repository;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryCustomImpl implements PaymentRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public BigDecimal sumForPeriod(OffsetDateTime from, OffsetDateTime to) {
        Aggregation agg = newAggregation(
                match(Criteria.where("timestamp").gte(from).lte(to)),
                group().sum("payment_amount").as("total")
        );

        AggregationResults<Document> res = mongoTemplate.aggregate(agg, "payments", Document.class);
        Document doc = res.getUniqueMappedResult();
        if (doc == null || doc.get("total") == null) {
            return BigDecimal.ZERO;
        }

        Object total = doc.get("total");
        if (total instanceof org.bson.types.Decimal128 d128) {
            return d128.bigDecimalValue();
        }
        return new BigDecimal(String.valueOf(total));
    }
}
