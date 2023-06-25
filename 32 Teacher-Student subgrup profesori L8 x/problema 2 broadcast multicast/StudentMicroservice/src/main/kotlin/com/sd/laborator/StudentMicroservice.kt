package com.sd.laborator

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.Socket
import kotlin.system.exitProcess

class StudentMicroservice {
    // intrebarile si raspunsurile sunt mentinute intr-o lista de perechi de forma:
    // [<INTREBARE 1, RASPUNS 1>, <INTREBARE 2, RASPUNS 2>, ... ]
    private lateinit var questionDatabase: MutableList<Pair<String, String>>
    private lateinit var messageManagerSocket: Socket
    private lateinit var broadcastManagerSocket: Socket
    private lateinit var multicastManagerSocket: Socket

    init {
        val databaseLines: List<String> =
            File("/home/stefan/Documents/Rezolvari Subiecte/32 Teacher-Student subgrup profesori L8/problema 2 broadcast multicast/StudentMicroservice/questions_database.txt").readLines()
        questionDatabase = mutableListOf()

        /*
         "baza de date" cu intrebari si raspunsuri este de forma:

         <INTREBARE_1>\n
         <RASPUNS_INTREBARE_1>\n
         <INTREBARE_2>\n
         <RASPUNS_INTREBARE_2>\n
         ...
         */
        for (i in 0..(databaseLines.size - 1) step 2) {
            questionDatabase.add(Pair(databaseLines[i], databaseLines[i + 1]))
        }
    }

    companion object Constants {
        // pentru testare, se foloseste localhost. pentru deploy, server-ul socket (microserviciul MessageManager) se identifica dupa un "hostname"
        // acest hostname poate fi trimis (optional) ca variabila de mediu
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANAGER_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
    }

    private fun subscribeToMessageManager() {
        try {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            messageManagerSocket.getOutputStream().write("student\n".toByteArray())
            println("M-am conectat la MessageManager!")
        } catch (e: Exception) {
            println("Nu ma pot conecta la MessageManager!")
            exitProcess(1)
        }
    }

    private fun subscribeToBroadcastManager() {
        try {
            broadcastManagerSocket = Socket("localhost", 1650)
            broadcastManagerSocket.getOutputStream().write("student\n".toByteArray())
            println("M-am conectat la BroadcastManager!")
        } catch (e: Exception) {
            println("Nu ma pot conecta la BroadcastManager!")
            exitProcess(1)
        }
    }

    private fun subscribeToMulticastManager() {
        try {
            multicastManagerSocket = Socket("localhost", 1660)
            multicastManagerSocket.getOutputStream().write("student\n".toByteArray())
            println("M-am conectat la MulticastManager!")
        } catch (e: Exception) {
            println("Nu ma pot conecta la MulticastManager!")
            exitProcess(1)
        }
    }

    private fun respondToQuestion(question: String): String? {
        questionDatabase.forEach {
            // daca se gaseste raspunsul la intrebare, acesta este returnat apelantului
            if (it.first == question) {
                return it.second
            }
        }
        return null
    }

    public fun run() {
        // microserviciul se inscrie in lista de "subscribers" de la MessageManager prin conectarea la acesta
        subscribeToMessageManager()
        subscribeToBroadcastManager()
        subscribeToMulticastManager()

        println("StudentMicroservice se executa pe portul: ${messageManagerSocket.localPort}")
        println("Se asteapta mesaje...")

        val messageManagerBufferReader = BufferedReader(InputStreamReader(messageManagerSocket.inputStream))
        val broadcastManagerBufferReader = BufferedReader(InputStreamReader(broadcastManagerSocket.inputStream))
        val multicastManagerBufferReader = BufferedReader(InputStreamReader(multicastManagerSocket.inputStream))


        GlobalScope.launch {
            while (true) {
                // se asteapta intrebari trimise prin intermediarul "MessageManager"
                val response = messageManagerBufferReader.readLine()

                if (response == null) {
                    // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                    println("Microserviciul MessageService (${messageManagerSocket.port}) a fost oprit.")
                    messageManagerBufferReader.close()
                    messageManagerSocket.close()
                    break
                }
                // se foloseste un thread separat pentru tratarea intrebarii primite
                GlobalScope.launch {
                    val (messageType, messageDestination, messageBody) = response.split(" ", limit = 3)

                    when (messageType) {
                        // tipul mesajului cunoscut de acest microserviciu este de forma:
                        // intrebare <DESTINATIE_RASPUNS> <CONTINUT_INTREBARE>
                        "intrebare" -> {
                            println("Am primit o intrebare de la $messageDestination: \"${messageBody}\"")
                            var responseToQuestion = respondToQuestion(messageBody)
                            responseToQuestion?.let {
                                responseToQuestion = "raspuns $messageDestination $it"
                                println("Trimit raspunsul: \"${response}\"")
                                messageManagerSocket.getOutputStream()
                                    .write((responseToQuestion + "\n").toByteArray())
                            }
                        }
                    }
                }
            }
        }

        GlobalScope.launch {
            // se asteapta intrebari trimise prin intermediarul "BroadcastManager"
            while (true) {
                val response = broadcastManagerBufferReader.readLine()

                if (response == null) {
                    // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                    println("Microserviciul BroadcastManager (${broadcastManagerSocket.port}) a fost oprit.")
                    broadcastManagerBufferReader.close()
                    broadcastManagerSocket.close()
                    break
                }
                // se foloseste un thread separat pentru tratarea intrebarii primite
                GlobalScope.launch {
                    val (messageType, messageDestination, messageBody) = response.split(" ", limit = 3)

                    when (messageType) {
                        // tipul mesajului cunoscut de acest microserviciu este de forma:
                        // intrebare <DESTINATIE_RASPUNS> <CONTINUT_INTREBARE>
                        "intrebare" -> {
                            println("Am primit o intrebare de la $messageDestination: \"${messageBody}\"")
                            var responseToQuestion = respondToQuestion(messageBody)
                            responseToQuestion?.let {
                                responseToQuestion = "raspuns $messageDestination $it"
                                println("Trimit raspunsul: \"${response}\"")
                                broadcastManagerSocket.getOutputStream()
                                    .write((responseToQuestion + "\n").toByteArray())
                            }
                        }
                    }
                }
            }
        }
        GlobalScope.launch {
            // se asteapta intrebari trimise prin intermediarul "MulticastManager"
            while (true) {
                val response = multicastManagerBufferReader.readLine()

                if (response == null) {
                    // daca se primeste un mesaj gol (NULL), atunci inseamna ca cealalta parte a socket-ului a fost inchisa
                    println("Microserviciul MulticastManager (${multicastManagerSocket.port}) a fost oprit.")
                    multicastManagerBufferReader.close()
                    multicastManagerSocket.close()
                    break
                }
                // se foloseste un thread separat pentru tratarea intrebarii primite
                GlobalScope.launch {
                    val (messageType, messageDestination, messageBody) = response.split(" ", limit = 3)

                    when (messageType) {
                        // tipul mesajului cunoscut de acest microserviciu este de forma:
                        // intrebare <DESTINATIE_RASPUNS> <CONTINUT_INTREBARE>
                        "intrebare" -> {
                            println("Am primit o intrebare de la $messageDestination: \"${messageBody}\"")
                            var responseToQuestion = respondToQuestion(messageBody)
                            responseToQuestion?.let {
                                responseToQuestion = "raspuns $messageDestination $it"
                                println("Trimit raspunsul: \"${response}\"")
                                multicastManagerSocket.getOutputStream()
                                    .write((responseToQuestion + "\n").toByteArray())
                            }
                        }
                    }
                }
            }
        }
        while(true) {} // asteapta la infinit si mentine corutinele pornite
    }
}
fun main(args: Array<String>) {
    val studentMicroservice = StudentMicroservice()
    studentMicroservice.run()
}