package benchmark

import Person
import com.budinverse.medusa.config.dbConfig
import com.budinverse.medusa.core.transactionAsync
import kotlin.system.measureTimeMillis

suspend fun main(args: Array<String>) {
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

    InsertAsync().runBenchmarkAsync()
}


class InsertAsync(override val name: String = "INSERT_ASYNC",
                  override val iter: Int = 5) : Benchmark {
    // language=MySQL
    private val query = "INSERT INTO medusa_test.Person(name, age) VALUES (?,?)"
    private val persons = generatePersons()

    private fun generatePersons(): List<Person> {
        val personList: ArrayList<Person> = arrayListOf()
        for (i in 1..1_000_0) {
            personList.add(Person(name = "person$i", age = i))
        }

        return personList
    }

    override suspend fun runBenchmarkAsync() {
        for (i in 1..iter) {
            val time = measureTimeMillis {
                val jobs = List(persons.size) { i ->
                    transactionAsync {
                        insert<Int> {
                            statement = query
                            values = arrayOf(persons[i].name, persons[i].age)
                        }
                    }
                }
                jobs.forEach { it.join() }
            }

            println("\u001B[33m[medusa_benchmark::$name]\u001B[0m:It took $time to benchmark. Iter: $i")
            println("\u001B[33m[medusa_benchmark::$name]\u001B[0m:=======================================================================================")
        }
    }

    override fun runBenchmark() {
        throw NotImplementedError()
    }
}



