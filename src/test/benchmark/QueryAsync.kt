package benchmark

import Person
import com.budinverse.medusa.config.dbConfig
import com.budinverse.medusa.core.transactionAsync
import kotlin.system.measureTimeMillis

suspend fun main(args: Array<String>) {
    dbConfig {
        databaseUser = "root"
        databasePassword = "12345"
        databaseUrl = "jdbc:mysql://localhost/medusa?useLegacyDatetimeCode=false&serverTimezone=UTC"
        driver = "com.mysql.cj.jdbc.Driver"
        connectionPool = connectionPool {
            minimumIdle = 10
            maximumPoolSize = 15
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
    }
    QueryAsync().runBenchmarkAsync()
}

class QueryAsync(override val name: String = "QUERY_ASYNC",
                 override val iter: Int = 5) : Benchmark {

    // language=MySQL
    private val stmt = "SELECT * FROM person LIMIT 10000"
    private val size = 1000

    override suspend fun runBenchmarkAsync() {
        for (i in 1..iter) {
            val time = measureTimeMillis {
                val jobs = List(size) {
                    transactionAsync {
                        queryList<Person> {
                            statement = stmt
                            type = ::Person
                        }
                    }
                }

                jobs.forEach { job ->
                    job.join()
                }
            }
            println("\u001B[33m[medusa_benchmark]\u001B[0m: It took $time ms to complete $name. Iter: $i")
        }
    }

    override fun runBenchmark() {
        throw NotImplementedError()
    }

}