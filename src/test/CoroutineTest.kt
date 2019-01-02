import com.budinverse.medusa.config.dbConfig
import com.budinverse.medusa.core.transaction
import com.budinverse.medusa.core.transactionAsync
import com.budinverse.medusa.models.ExecResult
import com.budinverse.medusa.models.TransactionResult.Err
import com.budinverse.medusa.models.TransactionResult.Ok
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


fun main(args: Array<String>) = runBlocking {
    dbConfig {
        databaseUser = "root"
        databasePassword = "12345"
        databaseUrl = "jdbc:mysql://localhost/medusa_test?useLegacyDatetimeCode=false&serverTimezone=UTC"
        driver = "com.mysql.cj.jdbc.Driver"
    }

    launch {
        queryPersonAsync()
    }

    queryPersonAsync()
}

suspend fun queryPersonAsync() {
    var person: Person? = null
    var person2: Person? = null
    val txn1 = transactionAsync {
        query<Person> {
            statement = DummyData.query
            values = arrayOf("budi")
            type = ::Person
        }
    }
    val txn2 = transactionAsync {
        query<Person> {
            statement = DummyData.query
            values = arrayOf("budi2")
            type = ::Person
        }
    }

    person = when (val results = txn1.await()) {
        is Ok -> results.res[0] as? Person
        is Err -> null
    }

    person2 = when (val results = txn2.await()) {
        is Ok -> results.res[0] as? Person
        is Err -> null
    }

    println("${System.currentTimeMillis()} : $person")
    println("${System.currentTimeMillis()} : $person2")
}


fun queryList() {
    lateinit var personList: List<Person>
    val txn = transaction {
        queryList<Person> {
            statement = "SELECT * FROM medusa_test.Person"
            type = ::Person
        }
    }

    when (txn) {
        is Ok -> personList = txn.res[0] as List<Person>
        is Err -> throw IllegalStateException()
    }


    println("${System.currentTimeMillis()} : $personList")
}

suspend fun insertAsync() {
    var exr: Int = -1
    var exr2: ExecResult<Int>? = null

    val ins1 = transactionAsync {
        insert<Int> {
            statement = "INSERT INTO medusa_test.Person(name, age) VALUES (?,?)"
            values = arrayOf("jeff111", 19)
        }
    }

    val ins2 = transactionAsync {
        insert<Int> {
            statement = "INSERT INTO medusa_test.Person(name, age) VALUES (?,?)"
            values = arrayOf("jeff111", 19)
        }
    }

    println(ins1.await())
    println(ins2.await())

    println(exr)
    println(exr2)

}
