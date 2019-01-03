package benchmark

import Person
import com.budinverse.medusa.config.dbConfig
import com.budinverse.medusa.core.transaction
import com.budinverse.medusa.core.transactionAsync
import com.budinverse.medusa.utils.get
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

suspend fun main(args: Array<String>) {
    dbConfig {
        databaseUser = "root"
        databasePassword = "12345"
        databaseUrl = "jdbc:mysql://localhost/medusa_test?useLegacyDatetimeCode=false&serverTimezone=UTC"
        driver = "com.mysql.cj.jdbc.Driver"
    }

    InsertBenchmarkAsync.runBenchmark()
    //InsertBenchmark.runBenchmark()
}

object InsertBenchmark {
    // language=MySQL
    private const val query = "INSERT INTO medusa_test.Person(name, age) VALUES (?,?)"

    private fun generatePersons(): List<Person> {
        val personList: ArrayList<Person> = arrayListOf()
        for (i in 1..1_00_00) {
            personList.add(Person(name = "person$i", age = i))
        }

        return personList
    }

    fun runBenchmark() {
        val persons = generatePersons()
        val time = measureTimeMillis {
            persons.forEach { person ->
                transaction {
                    insert<Int> {
                        statement = query
                        values = arrayOf(person.name, person.age)
                        type = {
                            it[1]
                        }
                    }
                }
            }
        }

        println("\u001B[33m[medusa]\u001B[0m: It took $time ms to complete benchmark")
    }
}

object InsertBenchmarkAsync {
    // language=MySQL
    private const val query = "INSERT INTO medusa_test.Person(name, age) VALUES (?,?)"

    private fun generatePersons(): List<Person> {
        val personList: ArrayList<Person> = arrayListOf()
        for (i in 1..1_000_0) {
            personList.add(Person(name = "person$i", age = i))
        }

        return personList
    }

    suspend fun runBenchmark() {
        val persons = generatePersons()
        val counter = AtomicInteger(0)
        val time = measureTimeMillis {
            val jobs = List(persons.size) { i ->
                transactionAsync {
                    insert<Int> {
                        statement = query
                        values = arrayOf(persons[i].name, persons[i].age)
                        type = {
                            it[1]
                        }
                    }
                    //counter.addAndGet(1)
                }
            }
            jobs.forEach { it.join() }
        }

        println("\u001B[33m[medusa]\u001B[0m: It took $time ms to complete insert async benchmark")
        //println("\u001B[33m[medusa]\u001B[0m: $counter insertions")
    }


}

