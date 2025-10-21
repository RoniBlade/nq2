package org.example.feature;

import com.evolveum.midpoint.audit.api.AuditService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExampleService {

    @Autowired(required = false)
    private AuditService auditService;

    public void doSomething() {
        if (auditService != null) {
            System.out.println("ExampleService called");
        } else {
            System.out.println("AuditService is not available.");
        }
        System.out.println("Example service is working!");
    }
}
