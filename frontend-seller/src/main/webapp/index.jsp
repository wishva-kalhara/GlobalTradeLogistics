<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GlobalTrade Logistics &mdash; Seller Portal</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
    <div id="guest-card" class="mx-auto hidden max-w-xl">
        <div class="rounded-2xl border border-gray-200 bg-white p-8 text-center shadow-sm">
            <span class="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-green-600 text-xl font-bold text-white">GT</span>
            <h1 class="text-2xl font-semibold text-gray-900">GlobalTrade Logistics &mdash; Seller Portal</h1>
            <p class="mt-2 text-gray-500">Sign in to manage your purchase orders and product offerings.</p>
            <a href="/seller/auth/login.jsp" class="mt-6 inline-block rounded-md bg-green-600 px-5 py-2.5 font-medium text-white hover:bg-green-700">Log in</a>
        </div>
    </div>

    <div id="dashboard-card" class="hidden">
        <h1 class="text-xl font-semibold text-gray-900">Welcome back<span id="dashboard-name"></span></h1>
        <p class="mt-1 text-sm text-gray-500">Here's how your purchase orders are looking.</p>

        <div id="alert-error" class="mt-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>

        <div class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
                <p class="text-sm text-gray-500">Total purchase orders</p>
                <p id="stat-total-pos" class="mt-1 text-2xl font-semibold text-gray-900">&mdash;</p>
            </div>
            <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
                <p class="text-sm text-gray-500">Open orders</p>
                <p id="stat-open-pos" class="mt-1 text-2xl font-semibold text-gray-900">&mdash;</p>
            </div>
            <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
                <p class="text-sm text-gray-500">Total revenue</p>
                <p id="stat-total-revenue" class="mt-1 text-2xl font-semibold text-gray-900">&mdash;</p>
            </div>
        </div>

        <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
                <h3 class="text-sm font-medium text-gray-700">Orders by status</h3>
                <canvas id="chart-orders-by-status" class="mt-3"></canvas>
            </div>
            <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
                <h3 class="text-sm font-medium text-gray-700">Revenue by product</h3>
                <p id="chart-revenue-empty" class="mt-3 hidden text-sm text-gray-500">No completed orders yet.</p>
                <canvas id="chart-revenue-by-product" class="mt-3"></canvas>
            </div>
        </div>

        <div class="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <a href="/seller/purchase-orders.jsp" class="block rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition hover:border-green-300 hover:shadow-md">
                <h2 class="font-medium text-gray-900">My Purchase Orders</h2>
                <p class="mt-1 text-sm text-gray-500">View orders GlobalTrade has placed with you.</p>
            </a>
            <a href="/seller/shipments/create.jsp" class="block rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition hover:border-green-300 hover:shadow-md">
                <h2 class="font-medium text-gray-900">Create Shipment</h2>
                <p class="mt-1 text-sm text-gray-500">Ship an open purchase order for customs pickup.</p>
            </a>
            <a href="/seller/shipments/list.jsp" class="block rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition hover:border-green-300 hover:shadow-md">
                <h2 class="font-medium text-gray-900">My Shipments</h2>
                <p class="mt-1 text-sm text-gray-500">Track your shipments through customs and delivery.</p>
            </a>
            <a href="/seller/products/add-offering.jsp" class="block rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition hover:border-green-300 hover:shadow-md">
                <h2 class="font-medium text-gray-900">Add Product Offering</h2>
                <p class="mt-1 text-sm text-gray-500">List a new product you can supply.</p>
            </a>
        </div>
    </div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.seller.session") || "null");
    if (session && session.token) {
        document.getElementById("dashboard-card").classList.remove("hidden");
        document.getElementById("dashboard-name").textContent = ", " + session.email;
        loadDashboard(session);
    } else {
        document.getElementById("guest-card").classList.remove("hidden");
    }

    async function loadDashboard(session) {
        const errorEl = document.getElementById("alert-error");
        try {
            const res = await fetch("/api/v1/purchase-orders", {
                headers: { "Authorization": "Bearer " + session.token },
            });
            if (res.status === 401) {
                localStorage.removeItem("gtl.seller.session");
                window.location.href = "/seller/auth/login.jsp";
                return;
            }
            if (!res.ok) {
                const data = await res.json().catch(function () { return {}; });
                throw new Error(data.error || ("status " + res.status));
            }
            render(await res.json());
        } catch (err) {
            errorEl.textContent = "Could not load your purchase orders: " + err.message;
            errorEl.classList.remove("hidden");
        }
    }

    function render(orders) {
        const openCount = orders.filter(function (po) { return !po.completed; }).length;
        const completedCount = orders.length - openCount;
        const totalRevenue = orders
            .filter(function (po) { return po.completed; })
            .reduce(function (sum, po) { return sum + po.totalPrice; }, 0);

        document.getElementById("stat-total-pos").textContent = orders.length;
        document.getElementById("stat-open-pos").textContent = openCount;
        document.getElementById("stat-total-revenue").textContent = "$" + totalRevenue.toFixed(2);

        new Chart(document.getElementById("chart-orders-by-status"), {
            type: "doughnut",
            data: {
                labels: ["Open", "Completed"],
                datasets: [{
                    data: [openCount, completedCount],
                    backgroundColor: ["rgba(37, 99, 235, 0.6)", "rgba(22, 163, 74, 0.6)"],
                }],
            },
            options: { plugins: { legend: { position: "bottom" } } },
        });

        const revenueByProduct = {};
        orders.filter(function (po) { return po.completed; }).forEach(function (po) {
            revenueByProduct[po.productName] = (revenueByProduct[po.productName] || 0) + po.totalPrice;
        });
        const productLabels = Object.keys(revenueByProduct);

        if (productLabels.length === 0) {
            document.getElementById("chart-revenue-empty").classList.remove("hidden");
            return;
        }

        new Chart(document.getElementById("chart-revenue-by-product"), {
            type: "bar",
            data: {
                labels: productLabels,
                datasets: [{
                    label: "Revenue",
                    data: productLabels.map(function (label) { return revenueByProduct[label]; }),
                    backgroundColor: "rgba(217, 119, 6, 0.6)",
                }],
            },
            options: { indexAxis: "y", plugins: { legend: { display: false } }, scales: { x: { beginAtZero: true } } },
        });
    }
</script>
</body>
</html>
