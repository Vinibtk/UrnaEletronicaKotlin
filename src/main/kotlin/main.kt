import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    EleitorRepository.inicializar()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Urna Eletrônica - Tablet Mode",
            state = rememberWindowState(width = 1024.dp, height = 768.dp)
        ) {
            MaterialTheme {
                TelaCampanhaTablet()
            }
        }
    }
}

@Composable
fun TelaCampanhaTablet() {
    var listaCandidatos by remember { mutableStateOf(EleitorRepository.listarTodosCandidatos()) }

    // Estados dos inputs
    var nomeInput by remember { mutableStateOf("") }
    var numeroInput by remember { mutableStateOf("") }
    var partidoInput by remember { mutableStateOf("") }
    var cargoInput by remember { mutableStateOf("") }

    var mensagemSistema by remember { mutableStateOf("") }
    var isErro by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(32.dp)) {

        // Lado esquerdo
        Column(modifier = Modifier.weight(0.6f).padding(end = 24.dp)) {
            Text("Painel da Campanha", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
            Text("Candidatos cadastrados no sistema", fontSize = 18.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            if (listaCandidatos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum candidato cadastrado ainda.", color = Color.Gray, fontSize = 22.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(listaCandidatos) { cand ->
                        Card(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(cand.nome, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("${cand.cargo} | ${cand.partido}", fontSize = 16.sp, color = Color.DarkGray)
                                }
                                Text(
                                    text = cand.numero,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64B5F6)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Lado direito
        Card(
            modifier = Modifier.weight(0.4f).fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(32.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text("Novo Candidato", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = nomeInput,
                    onValueChange = { nomeInput = it },
                    label = { Text("Nome do Candidato", fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // campo Numerico com validação por inteiros
                OutlinedTextField(
                    value = numeroInput,
                    onValueChange = { novoTexto ->
                        // Verifica se o tamanho é até 2 E se todos os caracteres são dígitos
                        if (novoTexto.length <= 2 && novoTexto.all { char -> char.isDigit() }) {
                            numeroInput = novoTexto
                        }
                    },
                    label = { Text("Número (2 dígitos)", fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = partidoInput,
                    onValueChange = { partidoInput = it },
                    label = { Text("Partido", fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = cargoInput,
                    onValueChange = { cargoInput = it },
                    label = { Text("Cargo", fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val resultado = EleitorRepository.cadastrarCandidato(nomeInput, numeroInput, partidoInput, cargoInput)
                        isErro = resultado.startsWith("ERRO")
                        mensagemSistema = resultado

                        if (!isErro) {
                            nomeInput = ""
                            numeroInput = ""
                            partidoInput = ""
                            cargoInput = ""
                            listaCandidatos = EleitorRepository.listarTodosCandidatos()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CADASTRAR", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (mensagemSistema.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isErro) Color(0xFFFFCDD2) else Color(0xFFC8E6C9),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = mensagemSistema,
                            color = if (isErro) Color(0xFFC62828) else Color(0xFF2E7D32),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}