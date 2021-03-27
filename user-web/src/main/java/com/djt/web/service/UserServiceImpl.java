package com.djt.web.service;




import com.djt.context.annotation.Service;
import com.djt.domain.User;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

@Service
public class UserServiceImpl implements UserService {

    @Resource(name = "bean/EntityManager")
    private EntityManager entityManager;


    @Override
    public User register(User user) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        // 主调用
        entityManager.persist(user);
        transaction.commit();
        return user;
    }

    @Override
    public boolean deregister(User user) {
        return false;
    }

    @Override
    public boolean update(User user) {
        return false;
    }

    @Override
    public User queryUserById(Long id) {
      return   entityManager.find(User.class,id);
    }

    @Override
    public User queryUserByNameAndPassword(String name, String password) {
        return null;
    }
}
