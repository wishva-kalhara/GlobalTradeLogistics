<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GlobalTrade Logistics — Staff Console</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
    <div id="guest-card" class="mx-auto hidden max-w-xl">
        <div class="rounded-2xl border border-gray-200 bg-white p-8 text-center shadow-sm">
            <span class="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-green-600 text-xl font-bold text-white">GT</span>
            <h1 class="text-2xl font-semibold text-gray-900">GlobalTrade Logistics — Staff Console</h1>
            <p class="mt-2 text-gray-500">Internal staff sign in with a one-time email code. Accounts are provisioned by an administrator.</p>
            <a href="/app/login.jsp" class="mt-6 inline-block rounded-md bg-green-600 px-5 py-2.5 font-medium text-white hover:bg-green-700">Log in</a>
        </div>
    </div>

    <div id="dashboard-card" class="hidden">
        <h1 class="text-xl font-semibold text-gray-900">Welcome back<span id="dashboard-name"></span></h1>
        <p class="mt-1 text-sm text-gray-500">Signed in as <span id="dashboard-role" class="font-medium text-gray-700"></span>.</p>
        <div id="dashboard-cards" class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2"></div>
        <p id="no-functions" class="mt-6 hidden text-sm text-gray-500">Your role doesn't have any console actions yet.</p>
    </div>
</main>

<script>
    // Same role → page mapping as nav.jsp's account menu.
    const ROLE_FUNCTIONS = {
        ADMIN: [
            { href: "/app/app-user-management.jsp", label: "Application User Management", description: "Onboard staff accounts, or register a customer/supplier directly." },
            { href: "/app/vendor-performance.jsp", label: "Vendor Performance Report", description: "See on-time delivery scores per supplier." },
            { href: "/app/inventory.jsp", label: "Warehouse Inventory", description: "Check current stock levels by warehouse." },
        ],
        COORDINATOR: [
            { href: "/app/purchase-orders/create.jsp", label: "Create Purchase Order", description: "Order more stock from a supplier." },
            { href: "/app/vendor-performance.jsp", label: "Vendor Performance Report", description: "See on-time delivery scores per supplier." },
            { href: "/app/inventory.jsp", label: "Warehouse Inventory", description: "Check current stock levels by warehouse." },
        ],
        WAREHOUSE_MANAGER: [
            { href: "/app/purchase-orders/record-grn.jsp", label: "Record GRN", description: "Confirm goods received against an open purchase order." },
            { href: "/app/inventory.jsp", label: "Warehouse Inventory", description: "Check current stock levels by warehouse." },
        ],
        CUSTOMS_AGENT: [
            { href: "/app/shipments/manage.jsp", label: "Manage Shipments", description: "Update shipment status and record customs clearance." },
        ],
    };

    const session = JSON.parse(localStorage.getItem("gtl.app.session") || "null");
    if (session && session.token) {
        document.getElementById("dashboard-card").classList.remove("hidden");
        document.getElementById("dashboard-name").textContent = ", " + session.email;
        document.getElementById("dashboard-role").textContent = session.role;

        const functions = ROLE_FUNCTIONS[session.role] || [];
        if (functions.length === 0) {
            document.getElementById("no-functions").classList.remove("hidden");
        } else {
            const container = document.getElementById("dashboard-cards");
            functions.forEach(function (fn) {
                const card = document.createElement("a");
                card.href = fn.href;
                card.className = "block rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition hover:border-green-300 hover:shadow-md";
                card.innerHTML =
                    "<h2 class=\"font-medium text-gray-900\">" + fn.label + "</h2>" +
                    "<p class=\"mt-1 text-sm text-gray-500\">" + fn.description + "</p>";
                container.appendChild(card);
            });
        }
    } else {
        document.getElementById("guest-card").classList.remove("hidden");
    }
</script>
</body>
</html>
