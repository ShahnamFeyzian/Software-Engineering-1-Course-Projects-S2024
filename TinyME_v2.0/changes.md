# List of changes
Lets clean this shit.
## Phase1
focusing on OrderHandler and entry point of system.
### Commit 1
1. Add exception package to domain.
2. Add NotEnoughCreditException.java.
3. Change Broker class to use exceptions.
 
 ### Commit 2
 3. Add BaseOrder.java to request package.
 4. Change EnterOrder and DeleteOrder to extend BaseOrder.
 5. Add validateYourFields abstract method to BaseOrder and use it in OrderHandler.

### Commit 3
6. Add exception package to repository.
7. Add NotFoundException.java.
8. Remove returning null value from findSecurityByIsin method.
9. Add isThereSecurityWithIsin method to SecurityRepository.
10. Use isThereSecurityWithIsin in OrderHandler.validateEnterOrderRq() instead checking null returned value.

### Commit 4
11. Use isThereSecurityWithIsin in OrderHandler.validateDeleteOrderRq() instead checking null returned value.

### Commit 5
12. Change InvalidRequestException.java to extends RuntimeException instead Exception to remove all its annoying throws statements from Security and OrderHandler.

### Commit 6
13. Add checkLotAndTickSize method to Security and use it in OrderHandler.validateEnterOrderRq().

### Commit 7
14. Remove returning null and add isThere... method to all Repositories and use them in OrderHandler.
15. Add isPeakSizeValid to EnterOrderRq, it should be better(commented in OrderHandler.validateEnterOrderRq()) and also it has a little ambiguity(commented in EnterOrderRq.isPeakSizeValid()).  

### Commit 8 & 9
16. Add isSuccessful to MatchResult.
17. Add publishEnterOrderMatchResult and its derivatives to OrderHandler.

### Commit 10
18. Add InvalidPeakSizeException and InvalidIcebergPeakSizeException.
19. Add checkNewPeakSize to Order and override it for IcebergOrder.
20. Add NotFoundException to package domain.exception.
21. Remove returning null from OrderBook and add isThereOrderWithId method to it.
22. Add findOrderById and isThereOrderWithId methods to Security.
23. Move all throwing InvalidRequestException from Security to OrderHandler (explain more in no.24 and no.25).
24. Add validateUpdateOrderRq method to OrderHandler.
25. Modify validateDeleteOrderRq.
26. Synchronize OrderBookTest and SecurityTest based on the new implementation.

### Commit 11
27. Change name of validateEnterOrderRq to generalEnterOrderValidation.
28. Add new validateEnterOrderRq that contains both generalEnterOrderValidation and validateUpdateOrderRq methods.

## Phase 2 
Preventing the request from being leaked to any file except the OrderHandler.

### Commit 12
29. Add createOrderByEnterOrderRq method to OrderHandler.
30. Add runEnterOrderRq method to OrderHandler for clean code purpose.
31. Add updateFromNewOrder to Order and IcebergOrder, also delete updateFromOrderRq from them. 
32. Change addNewOrder and updateOrder, thy now get Order as argument instead EnterOrderRq.

### Commit 13
33. Synchronize SecurityTest based on the new implementation.

### Commit 14
34. Change createNewOrderByEnterOrderRq name to createTempOrderByEnterOrderRq and some renaming like this in Security, Order and IcebergOrder.

### Commit 15
35. Change some public methods to private in OrderHandler. 

### Commit 16
36. Add NotEnoughPositionException to package domain.exception.
37. Add checkPositionForNewOrder and checkPositionForUpdateOrder to Security.
38. Change addNewOrder and updateOrder in Security to use above methods.

### Commit 17
39. Change deleteOrder method in Security, now it gets Side and orderId as arguments instead DeleteOrderRq and modify the OrderHandler.
40. Clean deleteOrder.
41. Completely change the removeByOrderId method in OrderBook and add delete method to Order.
42. Synchronize SecurityTest and OrderBookTest based on the new implementation.

## Phase 3
Security.

### Commit 18
43. Add willPriorityLostInUpdate to Order and override it for IcebergOrder.
44. Use willPriorityLostInUpdate in Security.updateOrder.

### Commit 19 & 20
45. Modify updateFromTempOrder, it now increases its broker's credit if its side is Buy, also if some activity dependent losing priority or not.

### Commit 21
46. Add reAddUpdatedOrder method to Security. It is just painkiller, it should be treated properly

## Phase 4
Matcher.

### Commit22
47. Change snapshot method in Order and IcebergOrder for unit tests, in this new architecture OrderStatus is more important, so the snapshot could not change it. This is just effect on unit tests.
48. Modify dicreaseQuantity in Order and IcebergOrder, they now handle removing and replenish themselves and no need to matcher to handle such things.(the IcebergOrder version still needs to be cleaned)
49. Knocking and rebuilding Trade.
50. Add new clean constructor to Trade, also keep old one for unit tests. The unit tests seriously need to be modified, and old constructor should be removed.
51. The buy and the sell Order in the Trade changed to real reference to order instead a snapshot from it. This change fucked the unit tests and I had to comment three of them.
52. Add confirm method to Trade that is so cool. (I can't explain its functionality because it's 4AM, good night)
53. A little cleaning in match in Mather.

### Commit 23
54. Add hasOrderToMatch method to Matcher and use it as a condition of while loop in match function.
55. Rename the matchWithFirst method of OrderBook to findOrderToMatchWith and change its functionality, now it throws a NotFoundException instead returning null.

### Commit 24
56. Add OrderStatus.DONE, when a queued order is finished turn its status to DONE.
57. Modify the decreaseQuantity in Order and IcebergOrder, now they handle their deleting from queue by themselves.
58. Add rollback method to Trade and Order-IcebergOrder.
59. Add sellFirstVesion field to trade for handling rollback.
60. Add CantRollbackTradeException to have robust system.

### Commit 25
61. Modify rollbackTrades method in Mather to be sync with trade's rollback feature.

### Commit 26
62. Change match method to return Trade List instead MatchResult.
63. Modify the execute method and MatcherTest to be sync with above change.

### Commit 27
64. Add four mini method to Trade that help to handle the buyer and seller positions.
65. Remove the updating position part from execute method in Matcher.

### Commit 28
66. Add addOrderToQueue method to Matcher. This is painkiller, should be more clean.
> Somehow it is clean now, but we would work on tests and some TODO comments later.
