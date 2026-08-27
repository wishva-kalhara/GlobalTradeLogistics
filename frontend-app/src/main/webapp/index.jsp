<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GlobalTrade Logistics &mdash; Staff Console</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
    <div id="guest-card" class="mx-auto hidden max-w-xl">
        <div class="rounded-2xl border border-gray-200 bg-white p-8 text-center shadow-sm">
            <span class="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-green-600 text-xl font-bold text-white">GT</span>
            <h1 class="text-2xl font-semibold text-gray-900">GlobalTrade Logistics &mdash; Staff Console</h1>
            <p class="mt-2 text-gray-500">Internal staff sign in with a one-time email code. Accounts are provisioned by an administrator.</p>
            <a href="/app/login.jsp" class="mt-6 inline-block rounded-md bg-green-600 px-5 py-2.5 font-medium text-white hover:bg-green-700">Log in</a>
        </div>
    </div>

    <div id="dashboard-card" class="hidden">
        <h1 class="text-xl font-semibold text-gray-900">Welcome back<span id="dashboard-name"></span></h1>
        <p class="mt-1 text-sm text-gray-500">Signed in as <span id="dashboard-role" class="font-medium text-gray-700"></span>.</p>
        <div id="dashboard-cards" class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2"></div>
        <p id="no-functions" class="mt-6 hidden text-sm text-gray-500">Your role doesn't have any console actions yet.</p>

        <div id="analytics-section" class="mt-10 hidden">
            <h2 class="text-lg font-semibold text-gray-900">Analytics</h2>
            <div id="analytics-error" class="mt-3 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>

            <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
                    <p class="text-sm text-gray-500">Total sales</p>
                    <p id="stat-total-sales" class="mt-1 text-2xl font-semibold text-gray-900">&mdash;</p>
                </div>
                <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
                    <p class="text-sm text-gray-500">Total orders</p>
                    <p id="stat-total-orders" class="mt-1 text-2xl font-semibold text-gray-900">&mdash;</p>
                </div>
            </div>

            <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
                <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
                    <h3 class="text-sm font-medium text-gray-700">Orders by status</h3>
                    <canvas id="chart-orders-by-status" class="mt-3"></canvas>
                </div>
                <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
                    <h3 class="text-sm font-medium text-gray-700">Top products by revenue</h3>
                    <canvas id="chart-top-products" class="mt-3"></canvas>
                </div>
                <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm lg:col-span-2">
                    <h3 class="text-sm font-medium text-gray-700">Vendor performance &mdash; on-time delivery rate</h3>
                    <p id="vendor-performance-empty" class="mt-3 hidden text-sm text-gray-500">No vendor performance reports yet &mdash; they're generated automatically once a week.</p>
                    <canvas id="chart-vendor-performance" class="mt-3"></canvas>
                </div>
            </div>
        </div>
    </div>
</main>

<script>
    // Same role -> page mapping as nav.jsp's account menu.
    const ROLE_FUNCTIONS = {
        ADMIN: [
            { href: "/app/app-user-management.jsp", label: "Application User Management", description: "Onboard staff accounts, or register a customer/supplier directly." },
            { href: "/app/inventory.jsp", label: "Warehouse Inventory", description: "Check current stock levels by warehouse." },
            { href: "/app/shipments/list.jsp", label: "Shipments", description: "See every shipment currently in progress." },
        ],
        COORDINATOR: [
            { href: "/app/purchase-orders/create.jsp", label: "Create Purchase Order", description: "Order more stock from a supplier." },
            { href: "/app/inventory.jsp", label: "Warehouse Inventory", description: "Check current stock levels by warehouse." },
            { href: "/app/shipments/list.jsp", label: "Shipments", description: "See every shipment currently in progress." },
        ],
        WAREHOUSE_MANAGER: [
            { href: "/app/purchase-orders/record-grn.jsp", label: "Record GRN", description: "Confirm goods received for a delivered shipment." },
            { href: "/app/inventory.jsp", label: "Warehouse Inventory", description: "Check current stock levels by warehouse." },
            { href: "/app/shipments/list.jsp", label: "Shipments", description: "See every shipment currently in progress." },
        ],
        CUSTOMS_AGENT: [
            { href: "/app/shipments/manage.jsp", label: "Manage Shipments", description: "Update shipment status and record customs clearance." },
            { href: "/app/shipments/list.jsp", label: "All Shipments", description: "See every shipment currently in progress." },
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
        if (session.role === "ADMIN" || session.role === "COORDINATOR") {
            loadAnalytics(session);
        }
    } else {
        document.getElementById("guest-card").classList.remove("hidden");
    }

    async function loadAnalytics(session) {
        document.getElementById("analytics-section").classList.remove("hidden");
        const errorEl = document.getElementById("analytics-error");
        const authHeaders = { "Authorization": "Bearer " + session.token };

        try {
            const [salesRes, vendorRes] = await Promise.all([
                fetch("/api/v1/admin/sales-summary", { headers: authHeaders }),
                fetch("/api/v1/admin/vendor-performance", { headers: authHeaders }),
            ]);
            if (salesRes.status === 401 || vendorRes.status === 401) {
                localStorage.removeItem("gtl.app.session");
                window.location.href = "/app/login.jsp";
                return;
            }
            if (!salesRes.ok) {
                const data = await salesRes.json().catch(function () { return {}; });
                throw new Error(data.error || ("status " + salesRes.status));
            }
            renderSales(await salesRes.json());

            if (vendorRes.ok) {
                renderVendorPerformance(await vendorRes.json());
            }
        } catch (err) {
            errorEl.textContent = "Could not load analytics: " + err.message;
            errorEl.classList.remove("hidden");
        }
    }

    function renderSales(summary) {
        document.getElementById("stat-total-sales").textContent = "$" + summary.totalSales.toFixed(2);
        document.getElementById("stat-total-orders").textContent = summary.totalOrders;

        const statusLabels = Object.keys(summary.ordersByStatus || {});
        new Chart(document.getElementById("chart-orders-by-status"), {
            type: "bar",
            data: {
                labels: statusLabels,
                datasets: [{
                    label: "Orders",
                    data: statusLabels.map(function (label) { return summary.ordersByStatus[label]; }),
                    backgroundColor: "rgba(22, 163, 74, 0.6)",
                }],
            },
            options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } },
        });

        const topProducts = summary.topProducts || [];
        new Chart(document.getElementById("chart-top-products"), {
            type: "bar",
            data: {
                labels: topProducts.map(function (p) { return p.productName || ("Product " + p.productId); }),
                datasets: [{
                    label: "Revenue",
                    data: topProducts.map(function (p) { return p.revenue; }),
                    backgroundColor: "rgba(37, 99, 235, 0.6)",
                }],
            },
            options: { indexAxis: "y", plugins: { legend: { display: false } }, scales: { x: { beginAtZero: true } } },
        });
    }

    function renderVendorPerformance(reports) {
        if (reports.length === 0) {
            document.getElementById("vendor-performance-empty").classList.remove("hidden");
            return;
        }

        const labels = [];
        const rates = [];
        reports.forEach(function (r) {
            const match = /\(([\d.]+)%\)/.exec(r.details || "");
            if (match) {
                labels.push("Supplier " + r.reference);
                rates.push(parseFloat(match[1]));
            }
        });

        if (labels.length === 0) {
            document.getElementById("vendor-performance-empty").classList.remove("hidden");
            return;
        }

        new Chart(document.getElementById("chart-vendor-performance"), {
            type: "bar",
            data: {
                labels: labels,
                datasets: [{
                    label: "On-time %",
                    data: rates,
                    backgroundColor: "rgba(217, 119, 6, 0.6)",
                }],
            },
            options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, max: 100 } } },
        });
    }
</script>
</body>
</html>
