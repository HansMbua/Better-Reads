package io.javabrains.Books;

import org.springframework.data.cassandra.repository.CassandraRepository;

public interface bookRepository extends CassandraRepository<Book,String> {
}
