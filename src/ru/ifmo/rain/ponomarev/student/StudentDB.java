package ru.ifmo.rain.ponomarev.student;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentQuery {

    private Comparator<Student> studentComparator =
            Comparator.comparing(Student::getLastName, String::compareTo)
                    .thenComparing(Student::getFirstName, String::compareTo)
                    .thenComparingInt(Student::getId);

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getStudentParametersStream(students, Student::getFirstName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getStudentParametersStream(students, Student::getLastName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getStudentParametersStream(students, Student::getGroup)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getStudentParametersStream(students, s -> String.format("%s %s", s.getFirstName(), s.getLastName()))
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getStudentParametersStream(students, Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Comparator.comparingInt(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortedStudentsBy(students, Comparator.comparingInt(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortedStudentsBy(students, studentComparator);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return sortedStudentsBy(filteredStudents(students, s -> s.getFirstName().equals(name)), studentComparator);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return sortedStudentsBy(filteredStudents(students, s -> s.getLastName().equals(name)), studentComparator);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return sortedStudentsBy(filteredStudents(students, s -> s.getGroup().equals(group)), studentComparator);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return getFilteredStudentsStream(students, s -> s.getGroup().equals(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)));
    }


    private Stream<String> getStudentParametersStream(List<Student> students, Function<Student, String> function) {
        return students.stream()
                .map(function);
    }

    private List<Student> sortedStudentsBy(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private Stream<Student> getFilteredStudentsStream(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream()
                .filter(predicate);
    }

    private List<Student> filteredStudents(Collection<Student> students, Predicate<Student> predicate) {
        return getFilteredStudentsStream(students, predicate)
                .collect(Collectors.toList());
    }
}
