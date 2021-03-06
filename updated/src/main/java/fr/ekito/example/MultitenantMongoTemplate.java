package fr.ekito.example;

import fr.ekito.example.domain.Domain;
import fr.ekito.example.domain.MultitenantEntity;
import fr.ekito.example.exception.NoDomainForRequestException;
import fr.ekito.example.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by arnaud on 08/12/14.
 */
public class MultitenantMongoTemplate extends MongoTemplate {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public MultitenantMongoTemplate(MongoDbFactory mongoDbFactory, MappingMongoConverter mappingMongoConverter) {
        super(mongoDbFactory, mappingMongoConverter);
    }

    @Override
    public <T> T findOne(Query query, Class<T> entityClass, String collectionName) {
        if (isMultitenantEntity(entityClass)) {
            injectCriteria(query);
        }
        return super.findOne(query, entityClass, collectionName);
    }

    @Override
    public <T> List<T> find(final Query query, Class<T> entityClass, String collectionName) {
        if (isMultitenantEntity(entityClass)) {
            injectCriteria(query);
        }
        return super.find(query, entityClass, collectionName);
    }

    private void injectCriteria(Query query) {
        Optional<Domain> currentDomain = SecurityUtils.getCurrentDomain();
        // check already existing present group criteria
        boolean criteriaAlreadyExists = query.getQueryObject().containsField("userDomain");

        //need inject criteria
        if (!criteriaAlreadyExists) {
            Domain domain = currentDomain.get();
            if (currentDomain.isPresent()) {
                query.addCriteria(where("userDomain").is(domain));
                log.info("inject domain {} in query {}", domain, query);
            } else {
                // no domain found
                throw new NoDomainForRequestException();
            }
        } else {
            log.warn("current domain is empty");
        }
    }

    private <T> boolean isMultitenantEntity(Class<T> entityClass) {
        boolean res = MultitenantEntity.class.isAssignableFrom(entityClass);
        log.info("isMultitenantEntity {} ? {}", entityClass, res);
        return res;
    }

}
