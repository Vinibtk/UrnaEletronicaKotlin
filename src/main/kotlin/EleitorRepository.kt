import java.sql.DriverManager
import java.sql.SQLException

// Atualizado com o campo 'partido' conforme o diagrama
data class Candidato(val id: Int, val nome: String, val numero: String, val partido: String, val cargo: String)

object EleitorRepository {
    private const val URL = "jdbc:sqlite:urna.db"

    fun inicializar() {
        DriverManager.getConnection(URL).use { conn ->
            val stmt = conn.createStatement()
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS candidatos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL,
                    numero INTEGER NOT NULL UNIQUE, 
                    partido TEXT NOT NULL,
                    cargo TEXT NOT NULL
                );
            """.trimIndent())
        }
    }

    fun listarTodosCandidatos(): List<Candidato> {
        val lista = mutableListOf<Candidato>()
        DriverManager.getConnection(URL).use { conn ->
            // Incluído 'partido' no SELECT
            val sql = "SELECT id, nome, numero, partido, cargo FROM candidatos ORDER BY cargo, numero"
            conn.prepareStatement(sql).use { pstmt ->
                val rs = pstmt.executeQuery()
                while (rs.next()) {
                    lista.add(
                        Candidato(
                            id = rs.getInt("id"),
                            nome = rs.getString("nome"),
                            numero = rs.getString("numero"),
                            partido = rs.getString("partido"), // Resgatando do banco
                            cargo = rs.getString("cargo")
                        )
                    )
                }
            }
        }
        return lista
    }

    // Método atualizado para receber o partido
    fun cadastrarCandidato(nome: String, numero: String, partido: String, cargo: String): String {
        if (nome.isBlank() || numero.isBlank() || partido.isBlank() || cargo.isBlank()) {
            return "ERRO: Preencha todos os campos."
        }

        return try {
            DriverManager.getConnection(URL).use { conn ->
                // INSERT atualizado com a coluna 'partido'
                val sql = "INSERT INTO candidatos (nome, numero, partido, cargo) VALUES (?, ?, ?, ?)"
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, nome)
                    pstmt.setString(2, numero)
                    pstmt.setString(3, partido)
                    pstmt.setString(4, cargo)
                    pstmt.executeUpdate()
                }
            }
            "SUCESSO: Candidato cadastrado!"
        } catch (e: SQLException) {
            if (e.message?.contains("UNIQUE") == true) {
                "ERRO: O número '$numero' já está reservado para outro candidato!"
            } else {
                "ERRO ao cadastrar: ${e.message}"
            }
        }
    }
}