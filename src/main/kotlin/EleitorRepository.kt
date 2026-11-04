import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Candidato(val id: Int, val nome: String, val numero: String, val cargo: String)

object DatabaseManager {
    private const val URL = "jdbc:sqlite:urna.db"

    fun inicializarBanco() {
        DriverManager.getConnection(URL).use { conn ->
            val stmt = conn.createStatement()
            // Criação das tabelas (mantendo o que já tínhamos)
            stmt.execute("CREATE TABLE IF NOT EXISTS eleitores (id INTEGER PRIMARY KEY AUTOINCREMENT, codigo_identificacao TEXT NOT NULL UNIQUE, ja_votou BOOLEAN NOT NULL DEFAULT 0);")
            stmt.execute("CREATE TABLE IF NOT EXISTS candidatos (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL, numero TEXT NOT NULL UNIQUE, cargo TEXT NOT NULL, partido TEXT);")
            stmt.execute("CREATE TABLE IF NOT EXISTS votos (id INTEGER PRIMARY KEY AUTOINCREMENT, id_eleitor INTEGER NOT NULL, id_governador INTEGER, id_presidente INTEGER, data_hora TEXT NOT NULL);")

            // Insere candidatos se a tabela estiver vazia
            val rs = stmt.executeQuery("SELECT COUNT(*) FROM candidatos")
            if (rs.next() && rs.getInt(1) == 0) {
                inserirCandidato(conn, "Candidato A", "10", "Governador", "Partido X")
                inserirCandidato(conn, "Candidato B", "20", "Governador", "Partido Y")
                inserirCandidato(conn, "Candidato C", "30", "Presidente", "Partido Z")
                inserirCandidato(conn, "Candidato D", "40", "Presidente", "Partido W")
            }

            // Insere um eleitor de teste se não existir (código: 12345)
            val rsEleitor = stmt.executeQuery("SELECT COUNT(*) FROM eleitores")
            if (rsEleitor.next() && rsEleitor.getInt(1) == 0) {
                stmt.execute("INSERT INTO eleitores (codigo_identificacao, ja_votou) VALUES ('12345', 0);")
            }
        }
    }

    private fun inserirCandidato(conn: Connection, nome: String, numero: String, cargo: String, partido: String) {
        val sql = "INSERT INTO candidatos (nome, numero, cargo, partido) VALUES (?, ?, ?, ?)"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, nome)
            pstmt.setString(2, numero)
            pstmt.setString(3, cargo)
            pstmt.setString(4, partido)
            pstmt.executeUpdate()
        }
    }

    // Retorna true se o eleitor pode votar, false se já votou ou não existe
    fun validarEleitor(codigo: String): Pair<Boolean, String> {
        DriverManager.getConnection(URL).use { conn ->
            val sql = "SELECT ja_votou FROM eleitores WHERE codigo_identificacao = ?"
            conn.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, codigo)
                val rs = pstmt.executeQuery()
                if (rs.next()) {
                    val jaVotou = rs.getBoolean("ja_votou")
                    return if (jaVotou) Pair(false, "Eleitor já votou!") else Pair(true, "Eleitor liberado.")
                }
            }
        }
        return Pair(false, "Código de eleitor não encontrado.")
    }

    // Busca o candidato digitado
    fun buscarCandidato(numero: String, cargo: String): Candidato? {
        DriverManager.getConnection(URL).use { conn ->
            val sql = "SELECT id, nome, numero, cargo FROM candidatos WHERE numero = ? AND cargo = ?"
            conn.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, numero)
                pstmt.setString(2, cargo)
                val rs = pstmt.executeQuery()
                if (rs.next()) {
                    return Candidato(rs.getInt("id"), rs.getString("nome"), rs.getString("numero"), rs.getString("cargo"))
                }
            }
        }
        return null // Retorna null se for Voto Nulo
    }

    // Salva os votos (Governador e Presidente) e atualiza o status do eleitor
    fun registrarVoto(codigoEleitor: String, idGov: Int?, idPres: Int?): Boolean {
        DriverManager.getConnection(URL).use { conn ->
            try {
                conn.autoCommit = false // Inicia transação

                val idEleitorSql = "SELECT id FROM eleitores WHERE codigo_identificacao = ?"
                var idEleitor = -1
                conn.prepareStatement(idEleitorSql).use { pstmt ->
                    pstmt.setString(1, codigoEleitor)
                    val rs = pstmt.executeQuery()
                    if (rs.next()) idEleitor = rs.getInt("id")
                }

                val dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

                val insertVoto = "INSERT INTO votos (id_eleitor, id_governador, id_presidente, data_hora) VALUES (?, ?, ?, ?)"
                conn.prepareStatement(insertVoto).use { pstmt ->
                    pstmt.setInt(1, idEleitor)
                    if (idGov != null) pstmt.setInt(2, idGov) else pstmt.setNull(2, java.sql.Types.INTEGER)
                    if (idPres != null) pstmt.setInt(3, idPres) else pstmt.setNull(3, java.sql.Types.INTEGER)
                    pstmt.setString(4, dataHora)
                    pstmt.executeUpdate()
                }

                val updateEleitor = "UPDATE eleitores SET ja_votou = 1 WHERE id = ?"
                conn.prepareStatement(updateEleitor).use { pstmt ->
                    pstmt.setInt(1, idEleitor)
                    pstmt.executeUpdate()
                }

                conn.commit() // Confirma a transação
                return true
            } catch (e: Exception) {
                conn.rollback()
                e.printStackTrace()
                return false
            }
        }
    }
}