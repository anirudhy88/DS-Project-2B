Group Messenger with Total and FIFO Ordering:-
----------------------------------------------- 
Step 1 : Implementing TOTAL AND FIFO Ordering gurantees
The app will multicast every user-entered message to all app instances(including the one that is sneding message)
The app will implement B-multicast and not R-multicast
The app will provide FIFO-Total ordering even under failurue.
Assumption(From requirement doc): There will be atmost one failure in the middle of execution.
Failure implies force closing app instance but not killing an enitre emulator instance.
When a failure happens, the app instance will never come back during run.
Achieves FIFO-Total ordering based a decentralized algorithm.(similar to ISIS)

Testing:
Two phases.
Phase one: Testing wihtout any failure.All delivery sequences shoould be same across all processes.
Phase two: Testing with a failure. Live nodes message should follow Total-FIFO ordering.

Requirements Document:
----------------------
PA 2 Part B Specification.pdf

Path to the main files:
-----------------------
DS-Project-2B/GroupMessenger2/app/src/main/java/edu/buffalo/cse/cse486586/groupmessenger2/

Name of the files:
------------------
1. GroupMessengerActivity.java
2. GroupMessengerProvider.java
3. OnPTestClickListener.java
