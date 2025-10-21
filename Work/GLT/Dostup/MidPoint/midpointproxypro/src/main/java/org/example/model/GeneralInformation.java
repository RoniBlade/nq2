package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralInformation <E, D , R extends JpaRepository<E, ?>, M>{

    public Class<?> entityType;
    public Class<?> dtoType;
    public Class<?> mapperType;
    public Class<? extends JpaRepository<E, ?>> repositoryType; // TODO добавить наследование

//    public E entityType;
//    public D dtoType;
//    public M mapperType;
//    public R repositoryType; // TODO добавить наследование

//    public GeneralInformation(
//            E entityType,
//            D dtoType,
//            M mapperType,
//            R repositoryType) {
//
//        this.entityType = entityType;
//        this.dtoType = dtoType;
//        this.mapperType = mapperType;
//        this.repositoryType = repositoryType;
//    }
}
