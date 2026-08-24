<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GlobalTrade Logistics</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
    <div id="guest-card" class="mx-auto hidden max-w-xl">
        <div class="rounded-2xl border border-gray-200 bg-white p-8 text-center shadow-sm">
            <span class="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-green-600 text-xl font-bold text-white">GT</span>
            <h1 class="text-2xl font-semibold text-gray-900">GlobalTrade Logistics</h1>
            <p class="mt-2 text-gray-500">Order industrial supplies from our catalog, shipped from our warehouses to you.</p>
            <div class="mt-6 flex flex-wrap justify-center gap-3">
                <a href="/products.jsp" class="rounded-md bg-green-600 px-5 py-2.5 font-medium text-white hover:bg-green-700">Browse Products</a>
                <a href="/auth/login.jsp" class="rounded-md border border-gray-300 px-5 py-2.5 font-medium text-gray-700 hover:bg-gray-50">Log in</a>
            </div>
            <p class="mt-4 text-sm text-gray-500">New here? <a href="/auth/sign-up.jsp" class="font-medium text-green-700 hover:underline">Create an account</a>.</p>
        </div>
    </div>

    <div id="dashboard-card" class="hidden">
        <h1 class="text-xl font-semibold text-gray-900">Welcome back<span id="dashboard-name"></span></h1>
        <p class="mt-1 text-sm text-gray-500">What would you like to do?</p>
        <div class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
            <a href="/products.jsp" class="block rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition hover:border-green-300 hover:shadow-md">
                <h2 class="font-medium text-gray-900">Browse Products</h2>
                <p class="mt-1 text-sm text-gray-500">Pick items and place a new order.</p>
            </a>
            <a href="/orders.jsp" class="block rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition hover:border-green-300 hover:shadow-md">
                <h2 class="font-medium text-gray-900">My Orders</h2>
                <p class="mt-1 text-sm text-gray-500">Track everything you've ordered.</p>
            </a>
            <a href="/me/update-profile.jsp" class="block rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition hover:border-green-300 hover:shadow-md">
                <h2 class="font-medium text-gray-900">Update Profile</h2>
                <p class="mt-1 text-sm text-gray-500">Keep your contact and shipping details current.</p>
            </a>
        </div>
    </div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.customer.session") || "null");
    if (session && session.token) {
        document.getElementById("dashboard-card").classList.remove("hidden");
        document.getElementById("dashboard-name").textContent = ", " + session.email;
    } else {
        document.getElementById("guest-card").classList.remove("hidden");
    }
</script>
</body>
</html>
