# Simple asset transfer issue reproducer

## Directory structure

* [cordapp-v3](cordapp-v3) - implementation using corda 3.3 open source
* [cordapp-v4](cordapp-v4) - implementation using Corda Enterprise 4.0 RC03 and Corda O/S 4.0.

These projects should be nearly identical. 
The [cordapp-v4](cordapp-v4) project can be switched between OS and CE using the commented and documented lines in [build.gradle](build.gradle).

## Contents of the projects
They both declare:

* A simple State, [`NoteState`](cordapp-v4/contracts-v1/src/main/kotlin/com/template/states/NoteState.kt) and contract, [`NoteContract`](cordapp-v4/contracts-v1/src/main/kotlin/com/template/contracts/NoteContract.kt)
* An issuance flow [`IssueNote`](cordapp-v4/workflows-v1/src/main/kotlin/com/template/flows/IssueNote.kt); and
* A transfer flow [`TransferNote`](cordapp-v4/workflows-v1/src/main/kotlin/com/template/flows/TransferNote.kt)

In the workflow tests for [v3](cordapp-v3/workflows-v1/src/test/kotlin/com/template/FlowTests.kt) and [v4](cordapp-v4/workflows-v1/src/test/kotlin/com/template/FlowTests.kt) we issue the note, check the balance against issuer's vault, transfer and check balance on both parties.

Also, there are similar driver-based tests in [cordapp-v3/../DriverBasedTest](cordapp-v3/workflows-v1/src/integrationTest/kotlin/com/template/DriverBasedTest.kt) and [cordapp-v4/../DriverBasedTest](cordapp-v4/workflows-v1/src/integrationTest/kotlin/com/template/DriverBasedTest.kt). These behave with the same results as the MockNetwork tests.

## How to reproduce in the shell

Given an initialised network using `deployNodes` the following script reproduces the issue in version 4.

## Shell script to reproduce this

### PartyA
------
1. `flow start GetNoteBalance`
  -> should return 0

2. `flow start IssueNote amount: "$1000"`
  -> will return StateRef string - copy this 

3. `flow start GetNoteBalance`
  -> should return 100000

4. `flow start TransferNote  noteStateRefString: "<paste stateref string>", newOwner: "O=PartyB,L=New York,C=US"`

5. `flow start GetNoteBalance`
  -> should return 0 but returns 100000 in Corda 4!

### PartyB
------
6. `flow start GetNoteBalance`
  -> should return 100000 but returns 0 in Corda 4!

## Result
This test works fine with in v3. It fails in CE4 RC03. It also fails in OS 4.0-RC07.

## Addendum

### Logs

The logs from the shell execution can be found [here](logs).
