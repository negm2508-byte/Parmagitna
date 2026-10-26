package com.papererp.api.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papererp.common.annotation.IdempotentOperation;
import com.papererp.common.context.TenantContext;
import com.papererp.common.exception.DomainException;
import com.papererp.core.domain.IdempotencyRecordEntity;
import com.papererp.core.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

@Aspect
@Component
public class IdempotencyAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotencyAspect(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(idempotentOperation)")
    public Object handleIdempotency(ProceedingJoinPoint pjp, IdempotentOperation idempotentOperation) throws Throwable {
        HttpServletRequest request = getCurrentHttpRequest();
        String idempotencyKey = null;

        if (request != null) {
            idempotencyKey = request.getHeader(idempotentOperation.headerName());
        }

        Object[] args = pjp.getArgs();
        Object payloadObject = null;
        for (Object arg : args) {
            if (arg != null && !(arg instanceof HttpServletRequest)) {
                payloadObject = arg;
                break;
            }
        }

        // Check if payload object has an idempotencyKey accessor (e.g. ExecuteSlittingRequest)
        if ((idempotencyKey == null || idempotencyKey.isBlank()) && payloadObject != null) {
            try {
                Method method = payloadObject.getClass().getMethod("idempotencyKey");
                Object val = method.invoke(payloadObject);
                if (val instanceof String s && !s.isBlank()) {
                    idempotencyKey = s;
                }
            } catch (NoSuchMethodException ignored) {
                // No idempotencyKey() method on request object
            }
        }

        // If no idempotency key was supplied at all, execute normally
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return pjp.proceed();
        }

        UUID tenantId = TenantContext.getRequiredTenantId();
        String payloadJson = payloadObject != null ? objectMapper.writeValueAsString(payloadObject) : "";

        // 1. Check existing record or reserve PENDING state
        Optional<IdempotencyRecordEntity> cachedRecord = idempotencyService.checkOrStart(tenantId, idempotencyKey, payloadJson);

        if (cachedRecord.isPresent()) {
            IdempotencyRecordEntity record = cachedRecord.get();
            log.info("Replaying idempotent response for tenant={}, key={}", tenantId, idempotencyKey);
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Class<?> returnType = signature.getReturnType();

            if (ResponseEntity.class.isAssignableFrom(returnType)) {
                // If return type is ResponseEntity, deserialize into method's generic type
                java.lang.reflect.Type genericType = signature.getMethod().getGenericReturnType();
                if (genericType instanceof java.lang.reflect.ParameterizedType pt) {
                    java.lang.reflect.Type bodyType = pt.getActualTypeArguments()[0];
                    var javaType = objectMapper.getTypeFactory().constructType(bodyType);
                    Object body = objectMapper.readValue(record.getResponseBody(), javaType);
                    return ResponseEntity.status(record.getResponseCode() != null ? record.getResponseCode() : 200).body(body);
                }
            }
            return objectMapper.readValue(record.getResponseBody(), returnType);
        }

        // 2. Execute target operation
        Object result;
        try {
            result = pjp.proceed();
        } catch (DomainException ex) {
            idempotencyService.fail(tenantId, idempotencyKey, ex.getHttpStatus(), ex.getMessage());
            throw ex;
        } catch (Throwable ex) {
            idempotencyService.fail(tenantId, idempotencyKey, 500, ex.getMessage());
            throw ex;
        }

        // 3. Complete and store response
        int statusCode = 200;
        String responseBodyJson;
        if (result instanceof ResponseEntity<?> re) {
            statusCode = re.getStatusCode().value();
            responseBodyJson = objectMapper.writeValueAsString(re.getBody());
        } else {
            responseBodyJson = objectMapper.writeValueAsString(result);
        }

        idempotencyService.complete(tenantId, idempotencyKey, statusCode, responseBodyJson);
        return result;
    }

    private HttpServletRequest getCurrentHttpRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }
}
