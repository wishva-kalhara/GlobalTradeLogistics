<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Add Product Offering — Seller Portal</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 class="text-xl font-semibold text-gray-900">Add Product Offering</h1>
        <p class="mt-1 text-sm text-gray-500">Register a product you can supply, which warehouse you'd deliver it to, and your typical lead time.</p>

        <div id="alert-error" class="mt-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
        <div id="alert-info" class="mt-4 hidden rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700"></div>

        <form id="offering-form" class="mt-6 space-y-4">
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Product</label>
                <select id="productId" required
                        class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30">
                    <option value="">Select a product&hellip;</option>
                </select>
            </div>
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Warehouse ID</label>
                <input type="number" id="warehouseId" min="1" value="1" required
                       class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
                <p class="mt-1 text-xs text-gray-400">The warehouse you'd deliver this product to.</p>
            </div>
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Lead time (days)</label>
                <input type="number" id="leadTimeInDays" min="0" required
                       class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
            </div>
            <button type="submit" class="w-full rounded-md bg-green-600 px-4 py-2.5 font-medium text-white hover:bg-green-700">Add Offering</button>
        </form>
    </div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.seller.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/seller/auth/login.jsp";
    }

    const errorEl = document.getElementById("alert-error");
    const infoEl = document.getElementById("alert-info");

    (async function loadProducts() {
        try {
            const res = await fetch("/api/v1/products");
            const products = await res.json();
            const select = document.getElementById("productId");
            products.forEach(function (p) {
                const option = document.createElement("option");
                option.value = p.productId;
                option.textContent = p.name;
                select.appendChild(option);
            });
        } catch (err) {
            errorEl.textContent = "Could not load products: " + err.message;
            errorEl.classList.remove("hidden");
        }
    })();

    document.getElementById("offering-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        errorEl.classList.add("hidden");
        infoEl.classList.add("hidden");

        const body = {
            productId: parseInt(document.getElementById("productId").value, 10),
            warehouseId: parseInt(document.getElementById("warehouseId").value, 10),
            leadTimeInDays: parseInt(document.getElementById("leadTimeInDays").value, 10),
        };

        try {
            const res = await fetch("/api/v1/suppliers/me/products", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + session.token,
                },
                body: JSON.stringify(body),
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
            infoEl.textContent = "Product offering added.";
            infoEl.classList.remove("hidden");
            document.getElementById("offering-form").reset();
            document.getElementById("warehouseId").value = 1;
        } catch (err) {
            errorEl.textContent = "Could not add product offering: " + err.message;
            errorEl.classList.remove("hidden");
        }
    });
</script>
</body>
</html>
