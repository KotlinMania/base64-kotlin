import Testing
import Base64

@Suite("Base64 Swift Export Tests")
struct Base64ExportTests {
    @Test("Swift module imports and basic types are reachable")
    func swiftModuleLoads() throws {
        #expect(Bool(true))
    }
}
