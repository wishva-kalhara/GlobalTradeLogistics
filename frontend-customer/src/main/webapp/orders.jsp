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
<main class="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
            <h1 class="text-xl font-semibold text-gray-900">My Orders</h1>
            <p class="mt-1 text-sm text-gray-500">Everything you've ordered, most recent first.</p>
        </div>
        <a href="/products.jsp"
           class="inline-flex items-center gap-1.5 rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-green-700">
            <svg class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10.75 4.75a.75.75 0 00-1.5 0v4.5h-4.5a.75.75 0 000 1.5h4.5v4.5a.75.75 0 001.5 0v-4.5h4.5a.75.75 0 000-1.5h-4.5v-4.5z"/>
            </svg>
            New order
        </a>
    </div>

    <div id="alert-error" class="mt-6 hidden items-start gap-2 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        <svg class="mt-0.5 h-4 w-4 flex-none" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M8.485 3.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 3.495zM10 6a.75.75 0 01.75.75v3.5a.75.75 0 01-1.5 0v-3.5A.75.75 0 0110 6zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd"/>
        </svg>
        <span id="alert-error-text"></span>
    </div>

    <div id="loading-skeleton" class="mt-6 space-y-4"></div>

    <div id="empty-state" class="mt-16 hidden flex-col items-center text-center">
        <svg class="h-12 w-12 text-gray-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 14.25l6-6m-6 0h4.5v4.5m4.5 6.75V21a2.25 2.25 0 01-2.25 2.25H6.75A2.25 2.25 0 014.5 21V7.5A2.25 2.25 0 016.75 5.25h5.69a2.25 2.25 0 011.591.659l4.309 4.31a2.25 2.25 0 01.659 1.59V21"/>
        </svg>
        <p class="mt-3 text-sm font-medium text-gray-900">No orders yet</p>
        <p class="mt-1 text-sm text-gray-500">Browse the catalog to place your first one.</p>
        <a href="/products.jsp" class="mt-4 inline-flex items-center rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700">Browse products</a>
    </div>

    <div id="orders-list" class="mt-6 space-y-4"></div>
</main>

<script>
    const STATUS_STYLES = {
        PLACED: "bg-blue-50 text-blue-700",
        SHIPPED: "bg-amber-50 text-amber-700",
        DELIVERED: "bg-green-50 text-green-700",
        CANCELLED: "bg-red-50 text-red-700",
    };

    const session = JSON.parse(localStorage.getItem("gtl.customer.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/auth/login.jsp";
    }

    const errorEl = document.getElementById("alert-error");
    const errorTextEl = document.getElementById("alert-error-text");
    const skeleton = document.getElementById("loading-skeleton");
    const emptyState = document.getElementById("empty-state");
    const list = document.getElementById("orders-list");

    const SKELETON_CARD =
        "<div class=\"animate-pulse rounded-2xl border border-gray-200 bg-white p-5\">" +
        "<div class=\"flex items-center justify-between\"><div class=\"h-4 w-24 rounded bg-gray-100\"></div><div class=\"h-5 w-20 rounded-full bg-gray-100\"></div></div>" +
        "<div class=\"mt-3 h-3 w-32 rounded bg-gray-100\"></div>" +
        "<div class=\"mt-4 h-3 w-full rounded bg-gray-100\"></div>" +
        "<div class=\"mt-2 h-3 w-2/3 rounded bg-gray-100\"></div>" +
        "</div>";
    skeleton.innerHTML = SKELETON_CARD.repeat(3);

    function showError(message) {
        errorTextEl.textContent = message;
        errorEl.classList.remove("hidden");
        errorEl.classList.add("flex");
    }

    function statusBadge(status) {
        const style = STATUS_STYLES[status] || "bg-gray-100 text-gray-700";
        return "<span class=\"rounded-full px-3 py-1 text-xs font-medium " + style + "\">" + status + "</span>";
    }

    function renderOrder(order) {
        let itemsHtml = "";
        order.items.forEach(function (item) {
            itemsHtml +=
                "<li class=\"flex items-center justify-between py-1.5\">" +
                "<span>" + item.qty + " &times; " + item.productName + "</span>" +
                "<span class=\"text-gray-500\">$" + (item.qty * item.unitPrice).toFixed(2) + "</span>" +
                "</li>";
        });

        const card = document.createElement("div");
        card.className = "rounded-2xl border border-gray-200 bg-white p-5 shadow-sm";
        card.innerHTML =
            "<div class=\"flex flex-wrap items-center justify-between gap-2\">" +
            "<h2 class=\"font-medium text-gray-900\">Order #" + order.orderId + "</h2>" +
            statusBadge(order.status) +
            "</div>" +
            "<p class=\"mt-1 text-xs text-gray-500\">" + new Date(order.orderedAt).toLocaleString() + "</p>" +
            "<ul class=\"mt-3 divide-y divide-gray-100 border-t border-gray-100 text-sm text-gray-700\">" + itemsHtml + "</ul>" +
            "<div class=\"mt-3 flex items-center justify-between border-t border-gray-100 pt-3\">" +
            "<span class=\"text-sm text-gray-500\">" + order.items.length + (order.items.length === 1 ? " item" : " items") + "</span>" +
            "<span class=\"text-sm font-semibold text-gray-900\">Total: $" + order.totalPrice.toFixed(2) + "</span>" +
            "</div>";
        return card;
    }

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
            if (!res.ok) {
                throw new Error("status " + res.status);
            }
            const orders = await res.json();
            skeleton.classList.add("hidden");

            if (orders.length === 0) {
                emptyState.classList.remove("hidden");
                emptyState.classList.add("flex");
                return;
            }

            orders
                .slice()
                .sort(function (a, b) { return new Date(b.orderedAt) - new Date(a.orderedAt); })
                .forEach(function (order) { list.appendChild(renderOrder(order)); });
        } catch (err) {
            skeleton.classList.add("hidden");
            showError("Could not load orders: " + err.message);
        }
    })();
</script>
</body>
</html>
