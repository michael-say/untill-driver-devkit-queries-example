# unTill(r) Driver Kit - Yes/No Query Example 

This example demonstrates how can user be prompted to answer "Yes" or "No" to some question during the EFT operation execution   

See [TestQueryDriver.java](src/main/java/com/mycompany/testq/TestQueryDriver.java) source code for details.

```java
ctx.getProgress().showQuery(guid, QUERY_ARE_YOU_SURE, ProgressQueryType.YES_NO, "Are you a sure?");
```