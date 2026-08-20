package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.wishva.globalTradeLogistics.core.remote.IUsersService;

import java.io.IOException;

@WebServlet("/api/v1/healthz")
public class HealthzController extends HttpServlet {

    @EJB
    private IUsersService usersService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.getWriter().write("Up and running... " + usersService.test());
    }
}
