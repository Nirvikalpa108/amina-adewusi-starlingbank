# Starling Bank Round-Up Feature

## Overview

This project implements a "round-up" feature for Starling Bank customers using the Starling public developer API. 
The feature calculates the total round-up amount from a customer's transactions over a week and transfers this amount into a savings goal to help customers save effortlessly.

---

## Features

- Fetches customer accounts using the Starling API.
- Retrieves transaction feed for a specified start date plus 6 days.
- Calculates the round-up amount by rounding each transaction up to the nearest pound.
- Creates or uses an existing savings goal.
- Transfers the round-up amount into the savings goal.
- Provides a CLI interface to trigger the round-up process.

---

## Technologies Used

- Scala 2
- http4s (HTTP client/server)
- Typelevel libraries (Cats, Cats Effect, etc.)
- Starling Bank Public API (no SDK used)

---

## Getting Started

### Prerequisites

Before you begin, make sure you have:

1. **Java Development Kit (JDK) 8 or higher** installed. Scala runs on the JVM.
2. **sbt (Scala Build Tool)** installed. This is used to build and run the project.
3. A **Starling Developer Account** with a sandbox customer and access token.

### Setup and Running Tests

Follow these steps to get started:

1. Go to the [Starling Developer Sandbox](https://developer.starlingbank.com/sandbox) and generate an **access token** for your sandbox customer.
2. Use the sandbox simulator to generate some transactions for your customer.
3. Choose a **start date** that falls within the timeline of the generated transactions (e.g., `"2025-08-29"`).
4. Set the following environment variables in your terminal (replace the token and date with your own):

   ```bash
   export STARLING_ACCESS_TOKEN="your_generated_access_token_here"
   export START_DATE="2025-08-29"


Start an sbt session by running:

`sbt`


From within the sbt interactive console, run the integration tests to trigger the round-up feature:

`integration / test`


To run unit tests, from the sbt console run:

`test`


This will run the unit tests which verify individual components and the integration tests which perform the full round-up flow against the sandbox API.

### How to Run the Program

The application is a command-line interface (CLI) tool that you run using sbt. It accepts a few command-line arguments to control its behavior:

#### Required Argument

`--start-date YYYY-MM-DD`

The start date of the week for which you want to calculate round-ups. This must be provided and should be a valid date.

#### Optional Arguments

`--goal-id <uuid>`

If you already have a savings goal UUID and want to use it instead of creating a new one, provide it here.

`--dry-run`

Runs the program without actually performing any transfers. Useful for testing or simulation.

### Running the Program

From your terminal, in the project directory, run:

`sbt`

Then, from within the sbt interactive shell, run:

`run --start-date 2025-08-29`

This will run the round-up process for the week starting on August 29, 2025.

To run with a specific savings goal UUID:

`run --start-date 2025-08-29 --goal-id 123e4567-e89b-12d3-a456-426614174000`

To run in dry-run mode (no transfers made):

`run --start-date 2025-08-29 --dry-run`

Dry-run doesn't support specifying a goal-id yet, but this is something that can be built in the future.

If you provide invalid arguments, the program will print an error message explaining the issue and show usage instructions.

### API Endpoints Used
- Accounts: Retrieve customer accounts.
- Transaction Feed: Retrieve transactions for the customer.
- Savings Goals: Create and transfer money to savings goals.

### Assumptions and Design Choices

Here are the main assumptions and design decisions made in this project:

#### Accounts
The app defaults to using the primary account for a user unless otherwise specified.
No logic to select between multiple accounts is implemented.
#### Transactions & Round-ups
- One round-up per transaction: once a transaction has been rounded up, it will not be rounded up again.
- Round-ups are calculated against the amount field of a FeedItem:
  - amount represents the settled amount in the account currency.
  - sourceAmount is relevant only for foreign currency transactions and is not used here.
  - totalFeeAmount relates to charges and is excluded from round-up calculations.
- Only SETTLED transactions are eligible for round-up; pending or authorised transactions are ignored.
#### Dates & Times
- API timestamps (transactionTime, settlementTime) are in UTC.
#### Currency
- All transactions are assumed to be in GBP.
#### Savings Goals
- A new savings goal is created if none exists.
- Round-ups are transferred into this goal.
- Goals can have any arbitrary target amount for the purposes of this test.
#### Transfers
- No pre-checks are made for account balance before making transfers.
- Success is determined solely by the API response status code (2xx indicates success; otherwise, failure).
#### Design Choices Summary
- Week definition: The app interprets “transactions for a given week” as the start date given plus 6 additional days.
- Savings goals: The app checks for an existing savings goal and reuses it if found. If none exists, it creates a new one. This avoids creating a new goal on every run, which would be unrealistic.

### Current State of Idempotency and State Management
At present, the app does not support idempotency due to limitations encountered when running in the sbt interactive console. Specifically, Cats Effect’s Ref—which is essential for managing atomic, thread-safe state—does not behave correctly in this environment. As a result, the app does not track savings goals or transfers in a way that prevents duplicate operations.

#### What This Means
- Savings goals may be created multiple times instead of being reused.
- Transfers may be duplicated because the app does not track which transfers have already been made.
- The app currently lacks the guarantees of exactly-once processing for round-ups and transfers.
#### Future Plans
- Once the app is deployed in a proper server environment (outside the sbt console), we will implement state management using Cats Effect’s Ref.
- This will enable atomic, thread-safe tracking of goals and transfers in-memory.
- Idempotency will be restored, ensuring that each round-up and transfer is processed exactly once per period.
#### Why Ref?
- Ref provides atomic, thread-safe mutable state inside a purely functional effect.
- This allows safe concurrent access and updates without race conditions.
- Using Ref means the app can track goals and transfers in-memory reliably without an external database.
- This design guarantees that round-ups and transfers are processed exactly once per period, ensuring idempotency.

### Core Logic: How Round-Ups Are Processed

The heart of the application is the processRoundups function. It orchestrates the entire round-up process step-by-step, ensuring correctness and idempotency. Here’s what it does, explained simply:

1. Fetch Customer Accounts:
Retrieves all accounts for the customer from the Starling API.

1. Select the Correct Account:
Picks the primary or correct account to work with.

1. Fetch Transactions for the Week:
Fetches all transactions for that account starting from the specified week’s start date.

1. Validate Transactions:
Checks transactions to ensure they are eligible for round-up (e.g., settled, debit transactions).

1. alculate the Round-Up Amount:
Calculates the total round-up amount by summing the difference between each transaction’s amount and the next whole pound.

1. Check for Zero Round-Up:
Stops processing if the total round-up amount is zero.

1. Get or Create Savings Goal:
Retrieves the existing savings goal UUID or creates a new one if none exists.

1. Persist the Goal UUID:
Saves the goal UUID in the app’s in-memory repository to avoid creating multiple goals.

1. Check Transfer Eligibility:
Checks if a transfer for this goal and week has already been made to avoid duplicates.

1. Perform the Transfer:
Transfers the round-up amount into the savings goal using the Starling API.

1. Record the Transfer:
Records the successful transfer in the app’s in-memory ledger.

1. Return the Result:
Returns the round-up amount and the savings goal UUID as confirmation.

### Configuration: Changing the Base URI

The application reads configuration values from the application.conf file located at src/main/resources/application.conf. This includes the Starling API base URI, access token, and other settings.

By default, the base URI is set to the Starling sandbox API:
```hocon
starling {
access-token = ${?STARLING_ACCESS_TOKEN}
base-uri = "https://api-sandbox.starlingbank.com"
}
```

If you need to change the API endpoint (for example, to point to a production environment or a different sandbox), you can override the base-uri value in this file or via environment variables.

For example, to use a different base URI, update application.conf:

```hocon
starling {
base-uri = "https://api.starlingbank.com"
}
```

This flexibility allows you to switch between environments without changing the code.

### Next Steps and Improvements

Currently, the application uses in-memory state management with Cats Effect's Ref to track savings goals and transfer records. This works well for a simple CLI app and testing but has limitations:

- Volatile state: All data is lost when the app stops.
- Single-instance only: Not suitable for distributed or multi-instance deployments.
- No idempotency support: Due to limitations running Ref in the sbt interactive console, idempotency is currently not implemented. This means duplicate goals or transfers may occur.

In the future, it would be beneficial to replace the in-memory Ref with a persistent database storage solution, such as a relational database (PostgreSQL, MySQL).
This would enable:

- Durable storage of goals and transfers
- Support for multiple instances or services
- Better fault tolerance and recovery
- Proper idempotency guarantees by tracking processed operations reliably.
- 
The repository interfaces (GoalRepository and TransferRepository) are designed to abstract the storage layer, so swapping the in-memory implementation for a database-backed one would require minimal changes to the rest of the application.

### Project Structure
- src/main/scala - Main application source code
- src/main/scala/clients - API client code for Starling endpoints
- src/main/scala/domain - Business logic for round-up calculation and transfers
- src/main/scala/repository - Storage services for idempotency
- src/main/scala/services - code that orchestrates other parts of the programme
- src/main/scala/cli - CLI interface for user interaction
- resources - Configuration files
- src/test/scala - unit tests
- integration - integration tests
