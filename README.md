# medusa - simpler jdbc
![Alt text](https://raw.githubusercontent.com/BudiNverse/medusa/master/preview.png)

 [ ![Download](https://api.bintray.com/packages/budinverse/utils/medusa/images/download.svg) ](https://bintray.com/budinverse/utils/medusa/_latestVersion)
 [![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)
 
> medusa is a jdbc-utilities library that is designed to reduce writing code pertaining to jdbc.
No more closing of connection manually, no spawning your own `preparedStatements`. 
This helps reduce bugs where connection is not closed and bugs where column number 
are wrong.All this in a lightweight library that leverages Kotlin's ability to write DSLs.
Medusa is not an ORM, it is just a utilities library to help you.

### Features
- Minimal use of reflection magic
- DSL which results in easier usage
- Asynchronous Transactions support (using Kotlin's coroutines)

### Changelog
#### [0.0.1 Experimental] 
- [x] Transaction Support
- [x] Async Transaction
- [x] DSL for setting of config
- [x] Standard database operations (query, queryList, insert, exec)
- [x] TransactionResult type

#### [0.0.2 Experimental - Possible breaking API changes]
- [x] Reduce API differentiation for operation that returns a key and no key. (0.0.2)
- [x] Extend config to have support for databases that cannot generate keys (0.0.2)
- [x] Database `Connection` pooling using HikariCP
 
### Planned changes/updates
- [ ] Batch processing
- [ ] Compile time generation of kotlin models based on database schema (0.0.3)
- [ ] Compile time generation of frequently used SQL statements. Eg. `INSERT INTO USER (email, username, passwordhash) VALUES (?,?,?)`(0.0.3)

### Misc TODOs
- [ ] Logo (why not, I can also design no kappa)
- [ ] Website (again, why not lmao)
- [ ] An actual fullstack example using Jetbrain's Ktor
- [ ] Some simple benchmark

--- 
## Usage
### Gradle
```groovy
repositories {
    jcenter()
}
dependencies {
  compile group: 'mysql', name: 'mysql-connector-java', version: '6.0.6' //depends on the driver you need
  compile 'com.budinverse.utils:medusa:<latest version>'
}
```
### Config
Setting up your `DatabaseConfig` via properties file
```kotlin
fun main(args: Array<String>) {
    DatabaseConfig.setConfig(configFileDir = "medusaConfig.properties")
}
```

Setting up your `DatabaseConfig` via built-in DSL
```kotlin
fun main(args: Array<String>) {
    dbConfig {
        databaseUser = "root"
        databasePassword = "12345"
        databaseUrl = "jdbc:mysql://localhost/medusa_test?useLegacyDatetimeCode=false&serverTimezone=UTC"
        driver = "com.mysql.cj.jdbc.Driver"
        connectionPool = connectionPool {
            minimumIdle = 10
            maximumPoolSize = 15
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
    }
}
```

---
## Examples
Assume that all examples has the following `User` class
```kotlin
data class User(val id: Int = 0,
                val name: String,
                val age: Int) {
        constructor(resultSet: ResultSet) : this(
                resultSet["id"],
                resultSet["name"],
                resultSet["age"]
        )
    }
```

### Query
A single query
```kotlin
fun getUser() = transaction {
        query<User> {
            //language=MySQL
            statement = "SELECT * FROM User WHERE name = ?"
            values = arrayOf("zeon222")
            type = ::User
        }
}

fun main(args: Array<String>) {
    val tx = getUser()
    when(tx) {
        is Ok -> println(tx.res[0] as User) // User(id=18, name=Zeon222, age=20)
        is Err -> println(tx.e)
    }
}

```
Querying list of objects
```kotlin
fun getUsers() = transaction {
        val users = queryList<User> {
            //language=MySQL
            statement = "SELECT * FROM User"
            type = ::User // make sure constructor is available!
        }
}

fun main(args: Array<String>) {
    val tx = getUsers()
    when(tx) {
        is Ok -> println(tx.res[0] as List<User>) // [User(id=18, name=Zeon222, age=20), User(id=20, name=Zeon333, age=19)]
        is Err -> println(tx.e)
    }
}

```

### Insert
```kotlin
fun insertUser(user: User): TransactionResult = transaction {
        exec {
            //language=MySQL
            statement = "INSERT INTO User (name, age) VALUES (?,?)"
            values = arrayOf(user.name, user.age)
        }
    }
    
fun runInsert() {
    val user = User("zeon000", 19)
    val res = insertUser(user)
    
    when (res) {
        is Ok -> /* Do smth on success */
        is Err -> /* Do smth on Err */
    }
}    
```

### Update/Delete
For deletion just change the statement
```kotlin
fun updateUser(user: user) = transaction {
        exec {
            //language=MySQL
            statement = "UPDATE medusa_test.user SET name = ? WHERE id = ?"
            values = arrayOf(user.name, 1)
        }
    }
    
fun runUpdate() {
        val user = User(name = "zeon000", age = 19)
        val res = updateUser(user)

        when (res) {
            is Ok -> /* Do smth on success */
            is Err -> /* Do smth on Err */
        }
    }
```

### Transaction Async
```kotlin
suspend fun queryUserAsync() {
    var user: User? = null
    var user2: User? = null

    val txn1 = transactionAsync {
        query<User> {
            statement = "SELECT * FROM medusa_test.Person WHERE name = ?"
            values = arrayOf("zeon111")
            type = ::Person
        }
    }

    val txn2 = transactionAsync {
        query<User> {
            statement = "SELECT * FROM medusa_test.Person WHERE name = ?"
            values = arrayOf("zeon111")
            type = ::Person
        }
    }

    // you have to await both transactions
    // before using person and person2
    // else there will be a data race
    // and it will print null
    // which is what was initialized with
    
    val awaitedTxn1 = txn1.await()
    val awaitedTxn2 = txn2.await()

    user = when (awaitedTxn1) {
        is Ok -> awaitedTxn1.res[0] as User
        is Err -> null
    }

    user2 = when (awaitedTxn2) {
        is Ok -> awaitedTxn2.res[0] as User
        is Err -> null
    }

    println("${System.currentTimeMillis()} : $user")
    println("${System.currentTimeMillis()} : $user2")
}

fun main(args: Array<String>) = runBlocking {
    launch {
        queryUserAsync()
    }
    queryUserAsync()
}
```

This will print
```
[medusa]: com.mysql.cj.jdbc.ClientPreparedStatement: SELECT * FROM medusa_test.User WHERE name = 'zeon111'. Warning(s): null. RowsUpdated: -1 
[medusa]: com.mysql.cj.jdbc.ClientPreparedStatement: SELECT * FROM medusa_test.User WHERE name = 'zeon111'. Warning(s): null. RowsUpdated: -1 
[medusa]: com.mysql.cj.jdbc.ClientPreparedStatement: SELECT * FROM medusa_test.User WHERE name = 'zeon111'. Warning(s): null. RowsUpdated: -1 
[medusa]: com.mysql.cj.jdbc.ClientPreparedStatement: SELECT * FROM medusa_test.User WHERE name = 'zeon111'. Warning(s): null. RowsUpdated: -1 
[medusa]: Closing connection: com.mysql.cj.jdbc.ConnectionImpl@1242cd18 
[medusa]: Closing connection: com.mysql.cj.jdbc.ConnectionImpl@21386018 
[medusa]: Closing connection: com.mysql.cj.jdbc.ConnectionImpl@4f66d092 
[medusa]: Closing connection: com.mysql.cj.jdbc.ConnectionImpl@6117a77f 
1539087990865 : User(id=2, name=zeon111, age=20)
1539087990865 : User(id=2, name=zeon111, age=20)
1539087990865 : User(id=2, name=zeon111, age=20)
1539087990865 : User(id=2, name=zeon111, age=20)
```

---
## Performance
> As of now, since medusa is still not 1.0 yet, performance is **NOT** a focus yet.However
if you looking for ballpark of how medusa performs, here they are

**Computer: Intel Core i7 3770k @ 4.3Ghz, 16GB @ 2000Mhz RAM, Samsung 850 EVO 500GB, Windows 10 Education, MySQL 8.0**
**Medusa: Minimum 10, Maximum 15 connections in pool, Coroutines dispatched on `Dispatchers.IO` from standard library**

Insert (asynchronous): 0.3531 ms/record

Query (asynchronous): 0.00009854 ms/record (1000 concurrent queries with 50k record/query)

**Computer: Macbook Pro (13-inch 2017) Intel Core i5 2.3Ghz, 8GB @ 2133Mhz**
**Medusa: Minimum 10, Maximum 15 connections in pool, Coroutines dispatched on `Dispatchers.IO` from standard library**

Insert (asynchronous): 0.1757 ms/record

Query (asynchronous): 0.00017972 ms/record (1000 concurrent queries with 50k record/query) 


