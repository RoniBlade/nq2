package com.hhAutoApply.latr.utlis;

import com.hhAutoApply.latr.utlis.CodeProvider;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class ConsoleCodeProvider implements CodeProvider {

    private final Scanner scanner = new Scanner(System.in);

    @Override
    public String getCode() {
        return scanner.next();
    }
}
