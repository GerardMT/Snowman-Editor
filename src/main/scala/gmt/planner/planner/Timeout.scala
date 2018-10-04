package gmt.planner.planner

class Timeout(private val threads: Seq[Thread], timeout: Int) extends Thread {

    override def run(): Unit = {
        Thread.sleep(timeout * 1000)

        for (t <- threads) {
            if (!t.isInterrupted)
            t.interrupt()
        }
    }
}
