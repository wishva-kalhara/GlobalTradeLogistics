<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Orders — GlobalTrade Logistics</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jspf" %>
<main class="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
    <h1 class="mb-1 text-xl font-semibold text-gray-900">My Orders</h1>
    <p class="mb-6 text-sm text-gray-500">Everything you've ordered, most recent first.</p>

    <div id="alert-error" class="mb-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>

    <div id="orders-list" class="space-y-4"></div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.customer.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/auth/login.jsp";
    }

    const errorEl = document.getElementById("alert-error");
    const list = document.getElementById("orders-list");

    (async function loadOrders() {
        try {
            const res = await fetch("/api/v1/orders", {
                headers: {"Authorization": "Bearer " + session.token},
            });
            if (res.status === 401) {
                localStorage.removeItem("gtl.customer.session");
                window.location.href = "/auth/login.jsp";
                return;
            }
            const orders = await res.json();
            if (orders.length === 0) {
                list.innerHTML = "<p class=\"text-sm text-gray-500\">No orders yet — <a href=\"/products.jsp\" class=\"font-medium text-green-700 hover:underline\">browse products</a> to place your first one.</p>";
                return;
            }

            orders.forEach(function (order) {
                const card = document.createElement("div");
                card.className = "rounded-2xl border border-gray-200 bg-white p-5 shadow-sm";
                let itemsHtml = "";
                order.items.forEach(function (item) {
                    itemsHtml += "<li>" + item.qty + " &times; " + item.productName + " ($" + item.unitPrice.toFixed(2) + " each)</li>";
                });
                card.innerHTML =
                    "<div class=\"flex items-center justify-between\">" +
                    "<h2 class=\"font-medium text-gray-900\">Order #" + order.orderId + "</h2>" +
                    "<span class=\"rounded-full bg-green-50 px-3 py-1 text-xs font-medium text-green-700\">" + order.status + "</span>" +
                    "</div>" +
                    "<p class=\"mt-1 text-xs text-gray-500\">" + new Date(order.orderedAt).toLocaleString() + "</p>" +
                    "<ul class=\"mt-3 list-inside list-disc text-sm text-gray-700\">" + itemsHtml + "</ul>" +
                    "<p class=\"mt-3 text-sm font-medium text-gray-900\">Total: $" + order.totalPrice.toFixed(2) + "</p>";
                list.appendChild(card);
            });
        } catch (err) {
            errorEl.textContent = "Could not load orders: " + err.message;
            errorEl.classList.remove("hidden");
        }
    })();
</script>
</body>
</html>
