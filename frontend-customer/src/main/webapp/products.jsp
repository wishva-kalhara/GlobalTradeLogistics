<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Products — GlobalTrade Logistics</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jspf" %>
<main class="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
    <h1 class="mb-1 text-xl font-semibold text-gray-900">Products</h1>
    <p class="mb-6 text-sm text-gray-500">Pick a quantity for anything you'd like to order, then place your order.</p>

    <div id="alert-error" class="mb-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
    <div id="alert-info" class="mb-4 hidden rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700"></div>

    <div id="product-grid" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3"></div>

    <div class="mt-8 flex justify-end">
        <button id="place-order-btn" type="button"
                class="rounded-md bg-green-600 px-5 py-2.5 font-medium text-white hover:bg-green-700">Place Order</button>
    </div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.customer.session") || "null");
    const errorEl = document.getElementById("alert-error");
    const infoEl = document.getElementById("alert-info");
    const grid = document.getElementById("product-grid");

    (async function loadProducts() {
        try {
            const res = await fetch("/api/v1/products");
            const products = await res.json();
            grid.innerHTML = "";
            products.forEach(function (p) {
                const card = document.createElement("div");
                card.className = "rounded-2xl border border-gray-200 bg-white p-5 shadow-sm";
                card.innerHTML =
                    "<h2 class=\"font-medium text-gray-900\">" + p.name + "</h2>" +
                    "<p class=\"mt-1 text-sm text-gray-500\">" + p.description + "</p>" +
                    "<p class=\"mt-3 text-sm text-gray-700\">$" + p.unitPrice.toFixed(2) + " &middot; " + p.availableQty + " in stock</p>" +
                    "<label class=\"mt-3 block text-sm font-medium text-gray-700\">Quantity</label>" +
                    "<input type=\"number\" min=\"0\" max=\"" + p.availableQty + "\" value=\"0\" data-product-id=\"" + p.productId + "\" " +
                    "class=\"qty-input mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30\"/>";
                grid.appendChild(card);
            });
        } catch (err) {
            errorEl.textContent = "Could not load products: " + err.message;
            errorEl.classList.remove("hidden");
        }
    })();

    document.getElementById("place-order-btn").addEventListener("click", async function () {
        errorEl.classList.add("hidden");
        infoEl.classList.add("hidden");

        if (!session || !session.token) {
            window.location.href = "/auth/login.jsp";
            return;
        }

        const items = [];
        document.querySelectorAll(".qty-input").forEach(function (input) {
            const qty = parseInt(input.value, 10);
            if (qty > 0) {
                items.push({productId: parseInt(input.dataset.productId, 10), qty: qty});
            }
        });

        if (items.length === 0) {
            errorEl.textContent = "Pick a quantity greater than zero for at least one product.";
            errorEl.classList.remove("hidden");
            return;
        }

        try {
            const res = await fetch("/api/v1/orders", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + session.token,
                },
                body: JSON.stringify({items: items}),
            });
            if (res.status === 401) {
                localStorage.removeItem("gtl.customer.session");
                window.location.href = "/auth/login.jsp";
                return;
            }
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                throw new Error(data.error || ("status " + res.status));
            }
            infoEl.textContent = "Order #" + data.orderId + " placed — total $" + data.totalPrice.toFixed(2) + ".";
            infoEl.classList.remove("hidden");
            document.querySelectorAll(".qty-input").forEach(function (input) { input.value = 0; });
        } catch (err) {
            errorEl.textContent = "Could not place order: " + err.message;
            errorEl.classList.remove("hidden");
        }
    });
</script>
</body>
</html>
