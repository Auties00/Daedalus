#include <google/protobuf/compiler/parser.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/descriptor.pb.h>
#include <google/protobuf/descriptor_database.h>
#include <google/protobuf/io/tokenizer.h>
#include <google/protobuf/io/zero_copy_stream_impl_lite.h>

// Force-link well-known type registrations so they appear in generated_pool()
#include <google/protobuf/any.pb.h>
#include <google/protobuf/api.pb.h>
#include <google/protobuf/duration.pb.h>
#include <google/protobuf/empty.pb.h>
#include <google/protobuf/field_mask.pb.h>
#include <google/protobuf/source_context.pb.h>
#include <google/protobuf/struct.pb.h>
#include <google/protobuf/timestamp.pb.h>
#include <google/protobuf/type.pb.h>
#include <google/protobuf/wrappers.pb.h>

// Reference default instances to prevent linker from stripping the registrations
static volatile const void* const kForceLink[] = {
    &google::protobuf::Any::default_instance(),
    &google::protobuf::Api::default_instance(),
    &google::protobuf::Duration::default_instance(),
    &google::protobuf::Empty::default_instance(),
    &google::protobuf::FieldMask::default_instance(),
    &google::protobuf::SourceContext::default_instance(),
    &google::protobuf::Struct::default_instance(),
    &google::protobuf::Timestamp::default_instance(),
    &google::protobuf::Type::default_instance(),
    &google::protobuf::DoubleValue::default_instance(),
};

class SilentErrorCollector : public google::protobuf::io::ErrorCollector {
public:
    bool has_errors = false;
    void RecordError(int, google::protobuf::io::ColumnNumber, absl::string_view) override { has_errors = true; }
    void RecordWarning(int, google::protobuf::io::ColumnNumber, absl::string_view) override {}
};

class SilentPoolErrorCollector : public google::protobuf::DescriptorPool::ErrorCollector {
public:
    void RecordError(absl::string_view, absl::string_view, const google::protobuf::Message*,
                     ErrorLocation, absl::string_view) override {}
};

// Database that serves the input file and falls back to the generated pool for imports
class InputAndGeneratedDatabase : public google::protobuf::DescriptorDatabase {
    google::protobuf::FileDescriptorProto input_;
public:
    void setInput(const google::protobuf::FileDescriptorProto& file) { input_ = file; }

    bool FindFileByName(absl::string_view filename,
                        google::protobuf::FileDescriptorProto* output) override {
        if (filename == input_.name()) {
            *output = input_;
            return true;
        }
        auto* file = google::protobuf::DescriptorPool::generated_pool()->FindFileByName(filename);
        if (file != nullptr) {
            file->CopyTo(output);
            return true;
        }
        return false;
    }

    bool FindFileContainingSymbol(absl::string_view symbol_name,
                                  google::protobuf::FileDescriptorProto* output) override {
        auto* file = google::protobuf::DescriptorPool::generated_pool()->FindFileContainingSymbol(symbol_name);
        if (file != nullptr) {
            file->CopyTo(output);
            return true;
        }
        return false;
    }

    bool FindFileContainingExtension(absl::string_view, int,
                                      google::protobuf::FileDescriptorProto*) override {
        return false;
    }
};

#ifdef _WIN32
#define EXPORT __declspec(dllexport)
#else
#define EXPORT __attribute__((visibility("default")))
#endif

extern "C" {

EXPORT int validate_proto(const char* content, int content_len) {
    google::protobuf::io::ArrayInputStream input(content, content_len);
    SilentErrorCollector parse_errors;
    google::protobuf::io::Tokenizer tokenizer(&input, &parse_errors);

    google::protobuf::FileDescriptorProto file_proto;
    google::protobuf::compiler::Parser parser;

    if (!parser.Parse(&tokenizer, &file_proto) || parse_errors.has_errors) {
        return 1;
    }

    if (!file_proto.has_name()) {
        file_proto.set_name("input.proto");
    }

    InputAndGeneratedDatabase db;
    db.setInput(file_proto);

    SilentPoolErrorCollector pool_errors;
    google::protobuf::DescriptorPool pool(&db, &pool_errors);

    return pool.FindFileByName(file_proto.name()) != nullptr ? 0 : 1;
}

}