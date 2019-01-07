# medusa - Simpler JDBC DSL
![Alt text](https://raw.githubusercontent.com/BudiNverse/medusa/master/preview.png)

 [ ![Download](https://api.bintray.com/packages/budinverse/utils/medusa/images/download.svg) ](https://bintray.com/budinverse/utils/medusa/_latestVersion)
 [![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)
 
> medusa is a jdbc-utilities library that is designed to reduce writing code pertaining to jdbc.
No more closing of connection manually, no spawning your own `preparedStatements`. 
This helps reduce bugs where connection is not closed and bugs where column number 
are wrong.All this in a lightweight library that leverages Kotlin's ability to write DSLs.
Medusa is not an ORM, it is just a utilities library to help you.

## Features 🚀
- Minimal use of reflection magic
- DSL which results in easier usage
- Asynchronous Transactions support (using Kotlin's coroutines)

## Changelog ⌚
#### [0.0.1 Experimental] 
- [x] Transaction Support
- [x] Async Transaction
- [x] DSL for setting of config
- [x] Standard database operations (query, queryList, insert, exec)
- [x] `TransactionResult` type

#### [0.0.2 Experimental - Breaking API changes]
- [x] Reduce API differentiation for operation that returns a key and no key. (0.0.2)
- [x] Extend config to have support for databases that cannot generate keys (0.0.2)
- [x] Database `Connection` pooling using [HikariCP](https://github.com/brettwooldridge/HikariCP)
- [x] Removed `TransactionResult` type
- [x] Updated to Kotlin 1.3 coroutines, dispatches on `Dispatcher.IO` by default for `transactionAsync` 
 
#### Planned changes/updates
- [ ] Batch processing
- [ ] Proper logger instead of `println`
- [ ] Compile time generation of kotlin models based on database schema
- [ ] Customizable `Connection` pool, apart from HikariCP 
- [ ] Compile time generation of frequently used SQL statements. Eg. `INSERT INTO USER (email, username, passwordhash) VALUES (?,?,?)`

#### Misc TODOs
- [ ] Logo (why not, I can also design no kappa)
- [ ] Website (again, why not lmao)
- [ ] An actual fullstack example using Jetbrain's Ktor
- [ ] Some simple benchmark

--- 
## Config ⚙️
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
## Examples 📖
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
    fun queryPerson(): Person {
        lateinit var personRes: ExecResult.SingleResult<Person>
        transaction {
            personRes = query {
                statement = "SELECT * FROM User WHERE name = ?"
                values = arrayOf("zeon111")
                type = ::Person
            }
        }

        return personRes.transformed // User(id=18, name=Zeon111, age=20)
    }

    
```

Querying list of objects
```kotlin
    fun queryListPerson(): ArrayList<Person> {
        lateinit var personList: ExecResult.ListResult<Person>
        transaction {
            personList = queryList {
                statement = "SELECT * FROM person"
                type = ::Person
            }
        }

        return personList.transformedList // [User(id=18, name=Zeon111, age=20), User(id=19, name=Zeon222, age=20)]
    }
```

### Insert
```kotlin
    fun insertTest(person: Person) {
        lateinit var ins: ExecResult.SingleResult<Int>
        transaction {
            ins = insert {
                statement = DummyData.insert
                values = arrayOf(person.name, person.age)
                type = {
                    it[1]
                }
            }
        }

        println(ins.transformed) // Auto generated PK
    }    
```

### Update/Delete
For deletion just change the statement
```kotlin
    fun updateTest(person: Person) {
        lateinit var execResult: ExecResult.SingleResult<Person>
        transaction {
            execResult = update {
                statement = "UPDATE person SET name = ?, age = ? WHERE id = ?"
                values = arrayOf(person.name, person.age, 1)
                type = ::Person
            }
        }

    }
```
---
## Performance 📊
> As of now, since medusa is still not 1.0 yet, performance is **NOT** a focus. The focus is to provide and ergonomic API.
However if you looking for ballpark of how medusa performs, here they are kappa

**Computer: Intel Core i7 3770k @ 4.3Ghz, 16GB @ 2000Mhz RAM, Samsung 850 EVO 500GB, Windows 10 Education, MySQL 8.0**

**Medusa: Minimum 10, Maximum 15 connections in pool, Coroutines dispatched on `Dispatchers.IO` from standard library**

Insert (asynchronous): Still benchmarking

Query (asynchronous): Still benchmarking

**Computer: Macbook Pro (13-inch 2017) Intel Core i5 2.3Ghz, 8GB @ 2133Mhz**

**Medusa: Minimum 10, Maximum 15 connections in pool, Coroutines dispatched on `Dispatchers.IO` from standard library**

Insert (asynchronous): Still benchmarking

Query (asynchronous): Still benchmarking 


