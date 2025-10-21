package org.example.models;

import lombok.*;
import java.util.*;

@Data
@AllArgsConstructor
public class Cart {

    List<Item> items = new ArrayList<Item>();

}
