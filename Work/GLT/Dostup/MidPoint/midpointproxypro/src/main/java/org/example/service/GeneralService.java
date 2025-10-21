package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterNode;
import org.example.util.RepositoryResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneralService <E, R extends JpaRepository<E, ?>, M> {

    private final RepositoryResolver repositoryResolver;

    public Page<Map<String, Object>> getQuery(String path, Pageable pageable){
        return repositoryResolver.executeQueryJdbcPaged(path, pageable);
    }

    public Page<Map<String, Object>> searchQuery(String path, Pageable pageable, List<FilterNode> filters){
        return repositoryResolver.executeQueryJdbcFilteredPaged(path, pageable, filters);
    }

//
//
//    public Page<E> getView(Pageable pageable) {
//        try {
//            Object repoInstance = context.getBean(repository);
//            Method findAllMethod = repository.getMethod("findAll", Pageable.class);
//            Object result = findAllMethod.invoke(repoInstance, pageable);
////            log.info(String.valueOf((OrgProfileEntity) result));
//            return (Page<E>) result;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Page.empty();
//        }
//    }
//    public Page<?> setDataAndGetView(Class<R> repositoryClass, Pageable pageable) {
//        this.repository = repositoryClass;
//        return getView(pageable);
//    }
}
