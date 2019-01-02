package benchmark

import Person
import com.budinverse.medusa.config.dbConfig
import com.budinverse.medusa.core.transaction
import com.budinverse.medusa.core.transactionAsync
import com.budinverse.medusa.models.TransactionResult
import kotlin.system.measureTimeMillis

suspend fun main(args: Array<String>) {
    dbConfig {
        databaseUser = "root"
        databasePassword = "12345"
        databaseUrl = "jdbc:mysql://localhost/medusa_test?useLegacyDatetimeCode=false&serverTimezone=UTC"
        driver = "com.mysql.cj.jdbc.Driver"
    }

    QueryBenchmark.runBenchmark()
    //QueryBenchmarkAsync.runBenchmark()
}


object QueryBenchmark {
    // language=MySQL
    private const val query = "SELECT * FROM medusa_test.Person"

    fun runBenchmark() {
        val listOfList: ArrayList<ArrayList<Person>> = arrayListOf()
        val time = measureTimeMillis {
            repeat(1000) {
                val res = transaction {
                    queryList<Person> {
                        statement = query
                        type = ::Person
                    }
                }

                when (res) {
                    is TransactionResult.Ok -> {
                        val data = res.res[0] as ArrayList<Person>
                        listOfList.add(data)
                    }
                    is TransactionResult.Err -> println(res.e)
                }
            }
        }

        println(listOfList.size)
        println("It took $time ms to complete benchmark")
    }
}

object QueryBenchmarkAsync {
    // language=MySQL
    private const val query = "SELECT * FROM medusa_test.Person"

    suspend fun runBenchmark() {
        val listOfList: ArrayList<ArrayList<Person>> = arrayListOf()
        val time = measureTimeMillis {
            val jobs = List(1000) {
                transactionAsync {
                    queryList<Person> {
                        statement = query
                        type = ::Person
                    }
                }
            }

            jobs.forEach { job ->
                job.join()
                when (val txn = job.await()) {
                    is TransactionResult.Ok -> {
                        val data = txn.res[0] as ArrayList<Person>
                        listOfList.add(data)
                    }
                    is TransactionResult.Err -> println(txn.e)
                }
            }
        }

        println(listOfList.size)
        println("\u001B[33m[medusa]\u001B[0m: It took $time ms to complete insert async benchmark")
    }
}