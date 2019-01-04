package benchmark

interface Benchmark {
    val name: String
    val iter: Int
    suspend fun runBenchmarkAsync()
    fun runBenchmark()
}