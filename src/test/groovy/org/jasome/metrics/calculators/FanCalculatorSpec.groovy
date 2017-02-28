package org.jasome.metrics.calculators

import org.jasome.input.Method
import org.jasome.input.Type
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.projectFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class FanCalculatorSpec extends Specification {

    def "properly counts fan-out"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class ClassA {
        
            public void printDouble(ClassB b) {
                System.out.println(b.getNumber() * getFactor());            
            }
            
            public int getFactor() {
                return 2;
            }

        }
        
        class ClassB {
            private int myNumber;
            
            public ClassB(int myNumber) {
                this.myNumber = myNumber;
            }
        
            public int getNumber() {
                return myNumber;
            }
        }
        '''

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type classA = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassA" }

        Method printDouble = (classA.getMethods() as List<Method>).find { method -> method.name == "public void printDouble(ClassB b)" }

        when:
        def result = new FanCalculator().calculate(printDouble);

        then:

        expect result, containsMetric("FOut", 3)
        expect result, containsMetric("Si", 9)
    }

    def "properly counts fan-in within a class"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class ClassA {
        
            public void printSquare(ClassB b) {
                System.out.println(b.getNumber() * b.getNumber());            
            }

        }
        
        class ClassB {
            private int myNumber;
            
            public ClassB(int myNumber) {
                this.myNumber = myNumber;
            }
        
            public int getNumber() {
                return myNumber;
            }
            
            public int getDoubleNumber() {
                return getNumber() + this.getNumber();
            }
        }
        '''

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type classB = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassB" }

        Method getDoubleNumber = (classB.getMethods() as List<Method>).find { method -> method.name == "public int getDoubleNumber()" }

        when:
        def result = new FanCalculator().calculate(getDoubleNumber);

        then:

        expect result, containsMetric("Fin", 0)
        expect result, containsMetric("Fout", 2)
        expect result, containsMetric("Si", 4)
    }

    def "properly counts fan-in outside of class"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class ClassA {
        
            public void printSquare(ClassB b) {
                System.out.println(b.getNumber() * b.getNumber());            
            }

        }
        
        class ClassB {
            private int myNumber;
            
            public ClassB(int myNumber) {
                this.myNumber = myNumber;
            }
        
            public int getNumber() {
                return myNumber;
            }
            
            public int getDoubleNumber() {
                return getNumber() + this.getNumber();
            }
        }
        '''

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type classB = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassB" }

        Method getNumber = (classB.getMethods() as List<Method>).find { method -> method.name == "public int getNumber()" }

        when:
        def result = new FanCalculator().calculate(getNumber);

        then:
        true

        //TODO: still don't have this working right
        //expect result, containsMetric("Fin", 4)
    }

    //TODO: tests for chained method calls

    //TODO: tests for class resolution on complex cross calls, lots of logic in the utils that aren't really tested here
    //TODO: check for toString() being called when using string concatenation?  is this doable?
}
