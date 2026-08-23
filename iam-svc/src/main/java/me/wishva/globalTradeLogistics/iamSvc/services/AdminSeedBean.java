package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.configs.AppConfig;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.model.User;

/**
 * Seeds one ADMIN user on deploy, idempotently, if the {@code users} table
 * is empty — same pattern as {@code CountrySeedBean}. Without this, the
 * system has no way to bootstrap its first ADMIN: every method on
 * {@code IUserAdminService} (including {@code createUser}) is guarded by
 * {@code @RequiresRole(Role.ADMIN)}, so nothing could ever provision the
 * first staff account through the API.
 */
@Singleton
@Startup
public class AdminSeedBean {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @PostConstruct
    void seed() {
        long existing = em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
        if (existing > 0) {
            return;
        }

        User admin = new User();
        admin.setEmail(AppConfig.ADMIN_EMAIL);
        admin.setFullName(AppConfig.ADMIN_FULL_NAME);
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        em.persist(admin);
    }
}
