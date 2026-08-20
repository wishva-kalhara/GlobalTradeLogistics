package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.ejb.Stateless;
import me.wishva.globalTradeLogistics.core.remote.IUsersService;

@Stateless
public class UserServiceBean implements IUsersService {

    @Override
    public boolean test() {
        return true;
    }
}
