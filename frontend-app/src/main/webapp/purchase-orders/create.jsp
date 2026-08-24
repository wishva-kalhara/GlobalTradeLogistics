<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Purchase Order — Staff Console</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 class="text-xl font-semibold text-gray-900">Create Purchase Order</h1>
        <p class="mt-1 text-sm text-gray-500">Order more stock from a supplier for a product that's running low.</p>

        <div id="alert-error" class="mt-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
        <div id="alert-info" class="mt-4 hidden rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700"></div>

        <form id="po-form" class="mt-6 space-y-4">
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Supplier ID</label>
                <input type="number" id="supplierId" min="1" required
                       class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
                <p class="mt-1 text-xs text-gray-400">Ask the supplier for their account's supplier ID, or check their onboarding email.</p>
            </div>
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Product</label>
                <select id="productId" required
                        class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30">
                    <option value="">Select a product&hellip;</option>
                </select>
            </div>
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Quantity</label>
                <input type="number" id="qty" min="1" required
                       class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
            </div>
            <button type="submit" class="w-full rounded-md bg-green-600 px-4 py-2.5 font-medium text-white hover:bg-green-700">Create Purchase Order</button>
        </form>
    </div>

    <div id="result-card" class="mt-6 hidden rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 class="font-medium text-gray-900">Purchase order created</h2>
        <dl class="mt-3 grid grid-cols-2 gap-3 text-sm">
            <div><dt class="text-gray-500">PO ID</dt><dd id="result-poId" class="font-medium text-gray-900"></dd></div>
            <div><dt class="text-gray-500">Product</dt><dd id="result-product" class="font-medium text-gray-900"></dd></div>
            <div><dt class="text-gray-500">Quantity</dt><dd id="result-qty" class="font-medium text-gray-900"></dd></div>
            <div><dt class="text-gray-500">Total price</dt><dd id="result-total" class="font-medium text-gray-900"></dd></div>
        </dl>
        <p class="mt-3 text-xs text-gray-400">Hand this PO ID to the warehouse manager — they'll need it to record the GRN when goods arrive.</p>
    </div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.app.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/app/login.jsp";
    } else if (session.role !== "COORDINATOR") {
        window.location.href = "/app/access-denied.jsp";
    }

    const errorEl = document.getElementById("alert-error");
    const infoEl = document.getElementById("alert-info");

    function showError(message) {
        errorEl.textContent = message;
        errorEl.classList.remove("hidden");
        infoEl.classList.add("hidden");
    }

    (async function loadProducts() {
        try {
            const res = await fetch("/api/v1/products");
            const products = await res.json();
            const select = document.getElementById("productId");
            products.forEach(function (p) {
                const option = document.createElement("option");
                option.value = p.productId;
                option.textContent = p.name + " (" + p.availableQty + " in stock)";
                select.appendChild(option);
            });
        } catch (err) {
            showError("Could not load products: " + err.message);
        }
    })();

    document.getElementById("po-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        errorEl.classList.add("hidden");
        infoEl.classList.add("hidden");
        document.getElementById("result-card").classList.add("hidden");

        const body = {
            supplierId: parseInt(document.getElementById("supplierId").value, 10),
            productId: parseInt(document.getElementById("productId").value, 10),
            qty: parseInt(document.getElementById("qty").value, 10),
        };

        try {
            const res = await fetch("/api/v1/purchase-orders", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + session.token,
                },
                body: JSON.stringify(body),
            });
            if (res.status === 401) {
                localStorage.removeItem("gtl.app.session");
                window.location.href = "/app/login.jsp";
                return;
            }
            const data = await res.json().catch(function () { return {}; });
            if (!res.ok) {
                throw new Error(data.error || ("status " + res.status));
            }

            infoEl.textContent = "Purchase order #" + data.poId + " created.";
            infoEl.classList.remove("hidden");

            document.getElementById("result-poId").textContent = "#" + data.poId;
            document.getElementById("result-product").textContent = data.productName;
            document.getElementById("result-qty").textContent = data.requestingQty;
            document.getElementById("result-total").textContent = "$" + data.totalPrice.toFixed(2);
            document.getElementById("result-card").classList.remove("hidden");

            document.getElementById("po-form").reset();
        } catch (err) {
            showError("Could not create purchase order: " + err.message);
        }
    });
</script>
</body>
</html>
