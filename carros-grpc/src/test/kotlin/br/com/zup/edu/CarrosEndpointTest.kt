package br.com.zup.edu

import io.grpc.Channel
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CarrosEndpointTest(val repository: CarroRepository,
                                  val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub) {

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    /**
     * 1. happy path - ok
     * 2. quando já existe carro com a placa - ok
     * 3. quando os dados de entrada são inválidos -ok
     */

    @Test
    fun `deve adicionar um novo carro`() {
        // cenário

        // ação
        val response = grpcClient.adicionar(CarroRequest.newBuilder()
                                                        .setModelo("Gol")
                                                        .setPlaca("HPX-1234")
                                                        .build())

        // validação
        with(response) {
            assertNotNull(id)
            assertTrue(repository.existsById(id)) // efeito colateral
        }


    }

    @Test
    fun `nao deve adicionar novo carro quando carro com placa ja existente`() {
        // cenário
        val existente = repository.save(Carro(modelo = "Palio", placa = "OIP-9876"))

        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(CarroRequest.newBuilder()
                                            .setModelo("Gol")
                                            .setPlaca(existente.placa)
                                            .build())
        }

        // validação
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("carro com placa existente", status.description)
        }

    }

    @Test
    fun `nao deve adicionar novo carro quando dados de entrada forem invalidos`() {
        // cenário

        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(CarroRequest.newBuilder()
                .setModelo("")
                .setPlaca("")
                .build())
        }

        // validação
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos", status.description)
        }
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub? {
            return CarrosGrpcServiceGrpc.newBlockingStub(channel)
        }

    }
}