-- Carga local para apresentação do sistema Mesa Certa.
-- Mantém os usuários cadastrados e recria somente os dados operacionais.

PRAGMA foreign_keys = ON;
BEGIN TRANSACTION;

UPDATE config
SET nome_restaurante = 'Mesa Certa',
    info_restaurante = 'Gestão de restaurante',
    numero_de_mesas = 12,
    taxa_de_servico = 10.0
WHERE id = 1;

-- Limpa primeiro as tabelas filhas para respeitar as chaves estrangeiras.
DELETE FROM venda_itens;
DELETE FROM vendas;
DELETE FROM pedidos_abertos;
DELETE FROM comandas_abertas;
DELETE FROM movimentacoes_estoque;
DELETE FROM itens_estoque;
DELETE FROM produtos;

-- Cardápio
INSERT INTO produtos
    (nome, descricao, preco, estoque, categoria_nome, tipo_classe, disponivel)
VALUES
    ('X-Burger', 'Pão brioche, carne, queijo e molho da casa', 24.90, 40, 'Lanche', 'item_cardapio', 1),
    ('X-Salada', 'Hambúrguer, queijo, alface, tomate e molho da casa', 28.90, 32, 'Lanche', 'item_cardapio', 1),
    ('Filé Executivo', 'Filé grelhado, arroz, feijão, salada e fritas', 38.50, 25, 'Prato', 'item_cardapio', 1),
    ('Picanha na Chapa', 'Picanha acebolada com fritas e farofa', 72.00, 18, 'Prato', 'item_cardapio', 1),
    ('Risoto de Camarão', 'Risoto cremoso com camarões', 49.90, 0, 'Prato', 'item_cardapio', 0),
    ('Batata Frita', 'Porção de batatas fritas crocantes', 18.00, 45, 'Porção', 'item_cardapio', 1),
    ('Suco de Laranja', 'Suco natural de laranja, 400 ml', 8.00, 36, 'Bebida', 'item_cardapio', 1),
    ('Coca-Cola Lata', 'Refrigerante Coca-Cola, 350 ml', 6.50, 48, 'Bebida', 'item_cardapio', 1),
    ('Água Mineral', 'Água mineral sem gás, 500 ml', 3.50, 60, 'Bebida', 'item_cardapio', 1),
    ('Caipirinha', 'Caipirinha tradicional de limão', 16.00, 24, 'Bebida', 'item_cardapio', 1),
    ('Café Espresso', 'Café espresso, 60 ml', 5.00, 50, 'Bebida', 'item_cardapio', 1),
    ('Pudim', 'Pudim de leite condensado', 9.90, 20, 'Sobremesa', 'item_cardapio', 1),
    ('Brownie com Sorvete', 'Brownie de chocolate com sorvete de creme', 13.50, 16, 'Sobremesa', 'item_cardapio', 1),
    ('Couvert Artístico', 'Couvert cobrado em noites com música ao vivo', 10.00, 9999, 'Serviço', 'servico', 1);

-- Estoque: três itens precisam de reposição para alimentar o indicador do painel.
INSERT INTO itens_estoque (nome, unidade_medida, quantidade)
VALUES
    ('Pão brioche', 'un.', 40),
    ('Carne para hambúrguer', 'kg', 4),
    ('Queijo muçarela', 'kg', 2),
    ('Batata congelada', 'kg', 18),
    ('Refrigerantes', 'un.', 48),
    ('Água mineral', 'un.', 60),
    ('Cervejas', 'un.', 36),
    ('Polpa de laranja', 'kg', 0),
    ('Café em grãos', 'kg', 8),
    ('Guardanapos', 'un.', 300);

INSERT INTO movimentacoes_estoque (item_id, tipo, quantidade, data_hora)
SELECT id, 'ENTRADA', quantidade, '2026-09-24T08:00:00'
FROM itens_estoque
WHERE quantidade > 0;

-- Comandas abertas exibidas no painel, nas mesas e na área de comandas.
INSERT INTO comandas_abertas (id, cliente_nome, mesa_numero, data_abertura)
VALUES
    (101, 'João', 3, '2026-09-24 10:05:00.000'),
    (102, 'Ana', 7, '2026-09-24 10:20:00.000'),
    (103, 'Carla', NULL, '2026-09-24 10:35:00.000'),
    (104, 'Rafael', 9, '2026-09-24 10:50:00.000'),
    (105, 'Beatriz', 11, '2026-09-24 11:05:00.000');

INSERT INTO pedidos_abertos
    (comanda_id, produto_nome, quantidade, observacao, horario, numero_lote, atendente_nome)
VALUES
    (101, 'X-Burger', 2, 'Um sem cebola', '2026-09-24 10:08:00.000', 1, 'admin'),
    (101, 'Coca-Cola Lata', 2, '', '2026-09-24 10:08:00.000', 1, 'admin'),
    (102, 'Picanha na Chapa', 1, 'Carne ao ponto', '2026-09-24 10:23:00.000', 1, 'wendell'),
    (102, 'Água Mineral', 2, '', '2026-09-24 10:23:00.000', 1, 'wendell'),
    (103, 'Filé Executivo', 1, 'Sem salada', '2026-09-24 10:38:00.000', 1, 'admin'),
    (103, 'Suco de Laranja', 1, 'Sem açúcar', '2026-09-24 10:38:00.000', 1, 'admin'),
    (104, 'Batata Frita', 2, '', '2026-09-24 10:53:00.000', 1, 'bruno'),
    (104, 'Caipirinha', 2, 'Pouco açúcar', '2026-09-24 10:53:00.000', 1, 'bruno'),
    (105, 'Pudim', 2, '', '2026-09-24 11:08:00.000', 1, 'abel'),
    (105, 'Café Espresso', 2, '', '2026-09-24 11:08:00.000', 1, 'abel');

-- Histórico de vendas concluídas, distribuído por datas e formas de pagamento.
INSERT INTO vendas
    (id, comanda_id, data_abertura, data_hora, cliente_nome, atendimento,
     forma_pagamento, subtotal, desconto, total, funcionario)
VALUES
    (1, 201, '2026-09-18 18:20:00.000', '2026-09-18 19:05:00.000', 'Marcos', 'Mesa 02', 'Dinheiro', 62.80, 0.00, 62.80, 'admin'),
    (2, 202, '2026-09-19 19:10:00.000', '2026-09-19 20:02:00.000', 'Fernanda', 'Mesa 05', 'Cartão de Débito', 90.00, 5.00, 85.00, 'wendell'),
    (3, 203, '2026-09-20 12:05:00.000', '2026-09-20 12:48:00.000', 'Lucas', 'Mesa 08', 'Pix presencial', 54.50, 0.00, 54.50, 'admin'),
    (4, 204, '2026-09-21 15:30:00.000', '2026-09-21 16:10:00.000', 'Patrícia', 'Cliente sem mesa', 'Cartão de Crédito', 29.80, 0.00, 29.80, 'bruno'),
    (5, 205, '2026-09-22 20:00:00.000', '2026-09-22 21:12:00.000', 'Gustavo', 'Mesa 04', 'Cartão de Crédito', 50.00, 0.00, 50.00, 'abel'),
    (6, 206, '2026-09-23 11:40:00.000', '2026-09-23 12:25:00.000', 'Renata', 'Mesa 06', 'Dinheiro', 35.40, 0.00, 35.40, 'admin'),
    (7, 207, '2026-09-23 19:15:00.000', '2026-09-23 20:30:00.000', 'Diego', 'Mesa 10', 'Pix presencial', 80.90, 0.00, 80.90, 'wendell'),
    (8, 208, '2026-09-24 08:10:00.000', '2026-09-24 09:00:00.000', 'Camila', 'Mesa 01', 'Cartão de Débito', 48.30, 3.00, 45.30, 'admin');

INSERT INTO venda_itens
    (venda_id, produto_nome, quantidade, preco_unitario, subtotal)
VALUES
    (1, 'X-Burger', 2, 24.90, 49.80),
    (1, 'Coca-Cola Lata', 2, 6.50, 13.00),
    (2, 'Picanha na Chapa', 1, 72.00, 72.00),
    (2, 'Batata Frita', 1, 18.00, 18.00),
    (3, 'Filé Executivo', 1, 38.50, 38.50),
    (3, 'Suco de Laranja', 2, 8.00, 16.00),
    (4, 'Pudim', 2, 9.90, 19.80),
    (4, 'Café Espresso', 2, 5.00, 10.00),
    (5, 'Caipirinha', 2, 16.00, 32.00),
    (5, 'Batata Frita', 1, 18.00, 18.00),
    (6, 'X-Burger', 1, 24.90, 24.90),
    (6, 'Água Mineral', 3, 3.50, 10.50),
    (7, 'Picanha na Chapa', 1, 72.00, 72.00),
    (7, 'Pudim', 1, 9.90, 9.90),
    (8, 'X-Salada', 1, 28.90, 28.90),
    (8, 'Suco de Laranja', 2, 8.00, 16.00),
    (8, 'Água Mineral', 1, 3.50, 3.50);

COMMIT;
