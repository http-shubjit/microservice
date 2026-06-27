# 🚀 How to Run and Test the Application

## Start the Services

Start the microservices in the following order:

1. **Eureka Server** – `http://localhost:8761`
2. **Config Server** – `http://localhost:8888`
3. **API Gateway** – `http://localhost:8080`
4. **Auth Server** – `http://localhost:8081`
5. **Data Service** – `http://localhost:8082`

> **Important:** Wait for each service to start successfully before starting the next one.

---

# Testing the Application

All APIs are accessible through the **API Gateway**.

Open the Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

---

## Step 1: Register a User

1. Open the **Auth Service** Swagger page.
2. Execute the **Register** API with valid user details.
3. After successful registration, log in (if required) and copy the generated **JWT token**.

---

## Step 2: Authorize the Data Service APIs

1. From the Swagger UI, switch to **Data Service** using the dropdown in the top-right corner.
2. Click the **Authorize** button.
3. Paste your JWT token.
4. Click **Authorize**, then **Close**.

Now all secured APIs are ready to use.

---

## Step 3: Place an Order

Execute the following APIs in order:

1. **Submit Product** (use a valid request body)
2. **Add to Cart**
3. **Initiate Order**

The **Initiate Order** API will return a **Razorpay Order ID**.

Copy this Order ID.

---

## Step 4: Complete the Payment

Open the following page in your browser:

```
http://localhost:8080/payment.html
```

Enter:

* Your registered email address
* The Razorpay Order ID generated in the previous step

Click **Pay**.

A Razorpay payment window will open.

Complete the payment using the Razorpay test payment options.

---

## Step 5: Verify the Order Status

Return to the **Data Service** Swagger page.

Execute the **Order** API.

If the payment was successful, the API will return the updated order status.

---

## Complete Flow

```
Register User
      ↓
Generate JWT Token
      ↓
Authorize Data Service
      ↓
Submit Product
      ↓
Add to Cart
      ↓
Initiate Order
      ↓
Receive Razorpay Order ID
      ↓
Open payment.html
      ↓
Complete Razorpay Payment
      ↓
Check Order Status
```
