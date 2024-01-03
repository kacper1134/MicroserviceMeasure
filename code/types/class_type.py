from typing import Dict, List

from code.types.field import Field
from code.types.field_relation import FieldRelation
from code.types.method import Method
from code.types.method_relation import MethodRelation


class Class:
    def __init__(self, name: str):
        self.name = name
        self.methods: Dict[str, Method] = {}
        self.fields: Dict[str, Field] = {}
        self.method_relations: Dict[str, List[MethodRelation]] = {}
        self.field_relations: Dict[str, List[FieldRelation]] = {}
        self.is_interface = False

    def add_method(self, method: Method):
        self.methods[method.name] = method

    def add_field(self, field: Field):
        self.fields[field.name] = field

    def add_method_relation(self, method_relation: MethodRelation):
        if method_relation.source_method not in self.method_relations:
            self.method_relations[method_relation.source_method] = []
        self.method_relations[method_relation.source_method].append(method_relation)

    def add_field_relation(self, field_relation: FieldRelation):
        if field_relation.source_method not in self.field_relations:
            self.field_relations[field_relation.source_method] = []
        self.field_relations[field_relation.source_method].append(field_relation)

    def __str__(self):
        methods_str = "\n".join([str(method) for method in self.methods.values()])
        return f"Class: {self.name}\nMethods:\n{methods_str}\nFields:\n{self.fields}"

