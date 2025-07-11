#!/bin/bash

# Test runner script for mingsha-javaagent-methodtime
# Supports unit tests, integration tests, performance tests, and coverage reports

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project directory
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to run unit tests
run_unit_tests() {
    print_status "Running unit tests..."
    mvn test -Dtest="**/*Test.java" -DfailIfNoTests=false
    print_success "Unit tests completed"
}

# Function to run integration tests
run_integration_tests() {
    print_status "Running integration tests..."
    mvn verify -Dtest="**/*IT.java" -DfailIfNoTests=false
    print_success "Integration tests completed"
}

# Function to run performance tests
run_performance_tests() {
    print_status "Running performance tests..."
    mvn test -Pperformance -Dtest="**/*PerformanceTest.java" -DfailIfNoTests=false
    print_success "Performance tests completed"
}

# Function to run coverage tests
run_coverage_tests() {
    print_status "Running coverage tests..."
    mvn test -Pcoverage -Dtest="**/*Test.java,**/*IT.java" -DfailIfNoTests=false
    print_success "Coverage tests completed"
}

# Function to generate coverage report
generate_coverage_report() {
    print_status "Generating coverage report..."
    mvn jacoco:report
    print_success "Coverage report generated at target/site/jacoco/index.html"
}

# Function to run JMH benchmarks
run_benchmarks() {
    print_status "Running JMH benchmarks..."
    mvn test -Pperformance -Dtest="**/*PerformanceTest.java" -DfailIfNoTests=false
    print_success "Benchmarks completed"
}

# Function to run all tests
run_all_tests() {
    print_status "Running all tests..."
    
    # Unit tests
    run_unit_tests
    
    # Integration tests
    run_integration_tests
    
    # Coverage tests
    run_coverage_tests
    
    # Generate coverage report
    generate_coverage_report
    
    print_success "All tests completed successfully"
}

# Function to clean up
cleanup() {
    print_status "Cleaning up..."
    mvn clean
    print_success "Cleanup completed"
}

# Function to show help
show_help() {
    echo "Usage: $0 [OPTION]"
    echo ""
    echo "Options:"
    echo "  unit          Run unit tests only"
    echo "  integration   Run integration tests only"
    echo "  performance   Run performance tests only"
    echo "  coverage      Run coverage tests and generate report"
    echo "  benchmarks    Run JMH benchmarks"
    echo "  all           Run all tests (default)"
    echo "  clean         Clean up build artifacts"
    echo "  help          Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 unit              # Run unit tests"
    echo "  $0 integration       # Run integration tests"
    echo "  $0 performance       # Run performance tests"
    echo "  $0 coverage          # Run coverage tests and generate report"
    echo "  $0 all               # Run all tests"
    echo "  $0 clean             # Clean up"
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if Maven is installed
    if ! command_exists mvn; then
        print_error "Maven is not installed. Please install Maven first."
        exit 1
    fi
    
    # Check if Java is installed
    if ! command_exists java; then
        print_error "Java is not installed. Please install Java first."
        exit 1
    fi
    
    # Check Java version
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 8 ]; then
        print_error "Java 8 or higher is required. Current version: $JAVA_VERSION"
        exit 1
    fi
    
    print_success "Prerequisites check passed"
}

# Function to show test summary
show_test_summary() {
    print_status "Test Summary:"
    echo "  - Unit tests: $(find src/test/java -name "*Test.java" -not -name "*IT.java" -not -name "*PerformanceTest.java" | wc -l | tr -d ' ') files"
    echo "  - Integration tests: $(find src/test/java -name "*IT.java" | wc -l | tr -d ' ') files"
    echo "  - Performance tests: $(find src/test/java -name "*PerformanceTest.java" | wc -l | tr -d ' ') files"
    echo "  - Coverage tests: $(find src/test/java -name "*Test.java" | wc -l | tr -d ' ') files"
}

# Main execution
main() {
    # Check prerequisites
    check_prerequisites
    
    # Show test summary
    show_test_summary
    
    # Parse command line arguments
    case "${1:-all}" in
        unit)
            run_unit_tests
            ;;
        integration)
            run_integration_tests
            ;;
        performance)
            run_performance_tests
            ;;
        coverage)
            run_coverage_tests
            generate_coverage_report
            ;;
        benchmarks)
            run_benchmarks
            ;;
        all)
            run_all_tests
            ;;
        clean)
            cleanup
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@" 