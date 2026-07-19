# 🚀 How to Run and Test the Application

## Start the Services

Start the microservices in the following order:

1. **Eureka Server** – `http://localhost:8761`
2. **Config Server** – `http://localhost:8888`
3. **API Gateway** – `http://localhost:8080`
4. **Auth Server** – `http://localhost:8081`
5. **Data Service** – `http://localhost:8082`
6. **Notification Service** – `http://localhost:8083`

> **Important:** Wait until each service is fully started and registered with Eureka before launching the next service.

---

# 📨 Registration & Notification Flow

The application now uses **RabbitMQ (CloudAMQP)** for asynchronous communication between microservices.

When a user registers:

1. The request is sent to the **API Gateway**.
2. The API Gateway forwards the request to the **Auth Service**.
3. The Auth Service validates and stores the user in the authentication database.
4. After successful registration, the Auth Service publishes a **UserRegistered** event to **RabbitMQ (CloudAMQP)**.
5. The **Notification Service** consumes the event.
6. The Notification Service sends a **Welcome Email** using **Spring Mail** and **Google Gmail SMTP**.

### Registration Event Flow

```text
                User
                  │
                  ▼
            API Gateway
                  │
                  ▼
            Auth Service
                  │
       Save User in Auth Database
                  │
                  ▼
 Publish UserRegistered Event
                  │
                  ▼
      RabbitMQ (CloudAMQP)
                  │
                  ▼
      Notification Service
                  │
      Spring Mail + Gmail SMTP
                  │
                  ▼
      📧 Welcome Email Sent
```

---

# 🧪 Testing the Application

All APIs are accessible through the **API Gateway**.

Open the Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

---

## Step 1: Register a User

1. Open the **Auth Service** from the Swagger dropdown.
2. Execute the **Register** API with valid user details.
3. After successful registration:

   * A welcome email will automatically be sent to the registered email address.
   * Log in using the registered credentials.
4. Copy the generated **JWT Token**.

---

## Step 2: Authorize Data Service APIs

1. Switch to **Data Service** from the Swagger dropdown.
2. Click **Authorize**.
3. Paste the JWT token.
4. Click **Authorize**, then **Close**.

You can now access all secured APIs.

---

## Step 3: Place an Order

Execute the following APIs in sequence:

1. **Submit Product**
2. **Add to Cart**
3. **Initiate Order**

The **Initiate Order** API returns a **Razorpay Order ID**.

Copy this Order ID.

---

## Step 4: Complete the Payment

Open the payment page:

```text
http://localhost:8080/payment.html
```

Enter:

* Registered Email Address
* Razorpay Order ID

Click **Pay**.

A Razorpay payment window will open.

Complete the payment using the Razorpay test credentials.

---

## Step 5: Verify the Order Status

Return to the **Data Service** Swagger page.

Execute the **Order** API.

If the payment is successful, the API returns the updated order status.

---

# 🔄 Complete Application Flow

```text
                    Register User
                          │
                          ▼
                    API Gateway
                          │
                          ▼
                    Auth Service
                          │
               Save User to Database
                          │
                          ▼
          Publish UserRegistered Event
                          │
                          ▼
             RabbitMQ (CloudAMQP)
                          │
                          ▼
              Notification Service
                          │
          Spring Mail + Gmail SMTP
                          │
                          ▼
             📧 Welcome Email Sent
                          │
                          ▼
                  Login & Get JWT
                          │
                          ▼
             Authorize Data Service
                          │
                          ▼
                  Submit Product
                          │
                          ▼
                    Add to Cart
                          │
                          ▼
                  Initiate Order
                          │
                          ▼
           Receive Razorpay Order ID
                          │
                          ▼
              Open payment.html
                          │
                          ▼
             Complete Razorpay Payment
                          │
                          ▼
               Verify Order Status
```

---

## 🛠️ Technologies Used

* Java 21
* Spring Boot
* Spring Security
* Spring Cloud Gateway
* Eureka Server
* Spring Cloud Config
* RabbitMQ
* CloudAMQP
* Spring AMQP
* Spring Mail
* Google Gmail SMTP
* PostgreSQL
* Redis
* JWT Authentication
* Razorpay Payment Gateway
* OpenAPI / Swagger
