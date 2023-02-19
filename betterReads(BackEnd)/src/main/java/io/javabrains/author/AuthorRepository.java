package io.javabrains.author;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AuthorRepository extends CassandraRepository<Author,String> {


}
